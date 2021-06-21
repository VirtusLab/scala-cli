package scala.build.bloop.bloopgun.internal

import java.io.{File, InputStream, OutputStream}
import java.net.{ConnectException, InetSocketAddress}
import java.nio.file.{Path, Paths}
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ScheduledExecutorService, ScheduledFuture}

import scala.build.bloop.bloopgun.{BloopgunLogger, BspConnection}
import scala.collection.JavaConverters._
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.{Failure, Success, Try}

import snailgun.TcpClient
import snailgun.logging.{Logger => SnailgunLogger}
import snailgun.protocol.Streams
import java.net.Socket

object Operations {

  /**
    * Checks whether a bloop server is running at this host / port.
    *
    * @param host
    * @param port
    * @param logger
    * @return Whether a server is running or not.
    */
  def check(
    host: String,
    port: Int,
    logger: BloopgunLogger
  ): Boolean =
    // inspired by https://github.com/scalacenter/bloop/blob/cbddb8baaf639a4e08ee630f1ebc559dc70255a8/bloopgun/src/main/scala/bloop/bloopgun/core/Shell.scala#L174-L202
    Util.withSocket { socket =>
      socket.setReuseAddress(true)
      socket.setTcpNoDelay(true)
      logger.debug(s"Attempting a connection to bloop server $host:$port ...")
      try {
        socket.connect(new InetSocketAddress(host, port))
        socket.isConnected()
      } catch {
        case _: ConnectException => false
      }
    }

  /**
    * Starts a new bloop server.
    *
    * @param host
    * @param port
    * @param javaPath
    * @param classPath
    * @param scheduler
    * @param waitInterval
    * @param timeout
    * @param logger
    * @return A future, that gets completed when the server is done starting (and can thus be used).
    */
  def startServer(
    host: String,
    port: Int,
    javaPath: String,
    javaOpts: Seq[String],
    classPath: Seq[Path],
    scheduler: ScheduledExecutorService,
    waitInterval: FiniteDuration,
    timeout: Duration,
    logger: BloopgunLogger
  ): Future[Unit] = {

    val command =
      Seq(javaPath) ++
      javaOpts ++
      Seq(
        "-cp", classPath.map(_.toString).mkString(File.pathSeparator),
        "bloop.Server",
        host,
        port.toString
      )
    val b = new ProcessBuilder(command: _*)
    b.redirectInput(ProcessBuilder.Redirect.PIPE)

    // https://stackoverflow.com/questions/55628999/java-processbuilder-how-to-suppress-output-instead-of-redirecting-it/55629297#55629297
    b.redirectOutput(Util.devNull)
    b.redirectError(Util.devNull)

    val p = b.start()
    p.getOutputStream.close()

    val promise = Promise[Unit]()

    def check0(f: => ScheduledFuture[_]): Runnable = {
      val start = System.currentTimeMillis()
      () =>
        val completionOpt =
          if (!p.isAlive()) Some(Failure(new Exception("Server didn't start")))
          else if (check(host, port, logger)) Some(Success(()))
          else if (timeout.isFinite && System.currentTimeMillis() - start > timeout.toMillis) Some(Failure(new Exception(s"Server didn't start after $timeout ms")))
          else None

        for (completion <- completionOpt) {
          try promise.complete(completion)
          catch {
            case _: IllegalStateException => // promise already completed, ignoring it
          }
          f.cancel(false)
        }
    }

    lazy val f: ScheduledFuture[_] = scheduler.scheduleAtFixedRate(
      logger.runnable("bloop-server-start-check")(check0(f)),
      0L,
      waitInterval.length,
      waitInterval.unit
    )

    f

    promise.future
  }

  /**
    * Opens a BSP connection to a running bloop server.
    *
    * Starts a thread to read output from the nailgun connection, and another one
    * to pass input to it.
    *
    * @param host
    * @param port
    * @param in
    * @param out
    * @param err
    * @param logger
    * @return A [[BspConnection]] object, that can be used to close the connection.
    */
  def bsp(
    host: String,
    port: Int,
    bspPort: Int,
    workingDir: Path,
    in: InputStream,
    out: OutputStream,
    err: OutputStream,
    logger: BloopgunLogger
  ): BspConnection = {

    val nailgunLogger: SnailgunLogger =
      new SnailgunLogger {
        val name: String = "bloop"
        val isVerbose: Boolean = true
        def debug(msg: String): Unit = logger.debug("nailgun debug: " + msg)
        def error(msg: String): Unit = logger.debug("nailgun error: " + msg)
        def warn(msg: String): Unit = logger.debug("nailgun warn: " + msg)
        def info(msg: String): Unit = logger.debug("nailgun info: " + msg)
        def trace(exception: Throwable): Unit = logger.debug("nailgun trace: " + exception.toString)
      }
    val stop0 = new AtomicBoolean
    val nailgunClient = TcpClient(host, port)
    val streams = Streams(in, out, err)

    val promise = Promise[Int]()
    val threadName = "bloopgun-nailgun-out"
    val runnable: Runnable = logger.runnable(threadName) { () =>
      val maybeRetCode = Try {
        nailgunClient.run(
          "bsp",
          Array("--protocol", "tcp", "--host", host, "--port", bspPort.toString),
          workingDir,
          sys.env.toMap,
          streams,
          nailgunLogger,
          stop0,
          interactiveSession = false
        )
      }
      try promise.complete(maybeRetCode)
      catch { case _: IllegalStateException => }
    }

    val snailgunThread = new Thread(runnable, threadName)
    snailgunThread.setDaemon(true)

    snailgunThread.start()

    new BspConnection {
      def address = s"$host:$bspPort"
      def openSocket() = new Socket(host, bspPort)
      val closed = promise.future
      def stop() = stop0.set(true)
    }
  }

}
