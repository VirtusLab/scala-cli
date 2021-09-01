package scala.build.blooprifle.internal

import java.io.{File, InputStream, IOException, OutputStream}
import java.net.{ConnectException, InetSocketAddress, Socket}
import java.nio.file.{Path, Paths}
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ScheduledExecutorService, ScheduledFuture}

import scala.build.blooprifle.{BloopRifleLogger, BspConnection}
import scala.collection.JavaConverters._
import scala.concurrent.{Future, Promise}
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}
import scala.util.{Failure, Success, Try}

import org.scalasbt.ipcsocket.NativeErrorException
import snailgun.TcpClient
import snailgun.logging.{Logger => SnailgunLogger}
import snailgun.protocol.Streams

object Operations {

  /** Checks whether a bloop server is running at this host / port.
    *
    * @param host
    * @param port
    * @param logger
    * @return
    *   Whether a server is running or not.
    */
  def check(
    host: String,
    port: Int,
    logger: BloopRifleLogger
  ): Boolean =
    // inspired by https://github.com/scalacenter/bloop/blob/cbddb8baaf639a4e08ee630f1ebc559dc70255a8/bloopgun/src/main/scala/bloop/bloopgun/core/Shell.scala#L174-L202
    Util.withSocket { socket =>
      socket.setReuseAddress(true)
      socket.setTcpNoDelay(true)
      logger.debug(s"Attempting a connection to bloop server $host:$port ...")
      try {
        socket.connect(new InetSocketAddress(host, port))
        socket.isConnected()
      }
      catch {
        case _: ConnectException => false
      }
    }

  /** Starts a new bloop server.
    *
    * @param host
    * @param port
    * @param javaPath
    * @param classPath
    * @param scheduler
    * @param waitInterval
    * @param timeout
    * @param logger
    * @return
    *   A future, that gets completed when the server is done starting (and can thus be used).
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
    logger: BloopRifleLogger
  ): Future[Unit] = {

    val command =
      Seq(javaPath) ++
        javaOpts ++
        Seq(
          "-cp",
          classPath.map(_.toString).mkString(File.pathSeparator),
          "bloop.Server",
          host,
          port.toString
        )
    val b = new ProcessBuilder(command: _*)
    b.redirectInput(ProcessBuilder.Redirect.PIPE)

    // https://stackoverflow.com/questions/55628999/java-processbuilder-how-to-suppress-output-instead-of-redirecting-it/55629297#55629297
    if (logger.bloopCliInheritStdout)
      b.redirectOutput(ProcessBuilder.Redirect.INHERIT)
    else
      b.redirectOutput(Util.devNull)

    if (logger.bloopCliInheritStderr)
      b.redirectError(ProcessBuilder.Redirect.INHERIT)
    else
      b.redirectError(Util.devNull)

    val p = b.start()
    p.getOutputStream.close()

    val promise = Promise[Unit]()

    def check0(f: => ScheduledFuture[_]): Runnable = {
      val start = System.currentTimeMillis()
      () =>
        val completionOpt =
          if (!p.isAlive())
            Some(Failure(new Exception("Server didn't start")))
          else if (check(host, port, logger))
            Some(Success(()))
          else if (timeout.isFinite && System.currentTimeMillis() - start > timeout.toMillis)
            Some(Failure(new Exception(s"Server didn't start after $timeout ms")))
          else
            None

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

  /** Opens a BSP connection to a running bloop server.
    *
    * Starts a thread to read output from the nailgun connection, and another one to pass input to
    * it.
    *
    * @param host
    * @param port
    * @param in
    * @param out
    * @param err
    * @param logger
    * @return
    *   A [[BspConnection]] object, that can be used to close the connection.
    */
  def bsp(
    host: String,
    port: Int,
    bspSocketOrPort: Either[Int, File],
    workingDir: Path,
    in: InputStream,
    out: OutputStream,
    err: OutputStream,
    logger: BloopRifleLogger
  ): BspConnection = {

    val nailgunLogger: SnailgunLogger =
      new SnailgunLogger {
        val name: String                      = "bloop"
        val isVerbose: Boolean                = true
        def debug(msg: String): Unit          = logger.debug("nailgun debug: " + msg)
        def error(msg: String): Unit          = logger.debug("nailgun error: " + msg)
        def warn(msg: String): Unit           = logger.debug("nailgun warn: " + msg)
        def info(msg: String): Unit           = logger.debug("nailgun info: " + msg)
        def trace(exception: Throwable): Unit = logger.debug("nailgun trace: " + exception.toString)
      }
    val stop0         = new AtomicBoolean
    val nailgunClient = TcpClient(host, port)
    val streams       = Streams(in, out, err)

    val promise    = Promise[Int]()
    val threadName = "bloop-rifle-nailgun-out"
    val protocolArgs = bspSocketOrPort match {
      case Left(bspPort) => Array("--protocol", "tcp", "--host", host, "--port", bspPort.toString)
      case Right(socket) => Array("--protocol", "local", "--socket", socket.getAbsolutePath)
    }
    val runnable: Runnable = logger.runnable(threadName) { () =>
      val maybeRetCode = Try {
        nailgunClient.run(
          "bsp",
          protocolArgs,
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
      def address = bspSocketOrPort match {
        case Left(bspPort) => s"$host:$bspPort"
        case Right(socket) => "local:" + socket.toURI.toASCIIString.stripPrefix("file:")
      }
      def openSocket() = bspSocketOrPort match {
        case Left(bspPort) =>
          new Socket(host, bspPort)
        case Right(socketFile) =>
          var count          = 0
          val period         = 50.millis
          val maxWait        = 10.seconds
          val maxCount       = (maxWait / period).toInt
          var socket: Socket = null
          while (
            !socketFile.exists() && socket == null && count < maxCount && closed.value.isEmpty
          ) {
            logger.debug {
              if (socketFile.exists())
                s"BSP connection $socketFile found but not open, waiting $period"
              else
                s"BSP connection at $socketFile not found, waiting $period"
            }
            Thread.sleep(period.toMillis)
            if (socketFile.exists()) {
              socket =
                try {
                  new org.scalasbt.ipcsocket.UnixDomainSocket(socketFile.getAbsolutePath, true)
                }
                catch {
                  case ex: IOException
                      if ex.getCause.isInstanceOf[NativeErrorException] &&
                        ex.getCause.asInstanceOf[NativeErrorException].returnCode == 111 =>
                    logger.debug(s"Error when connecting to $socketFile: ${ex.getMessage}")
                    null
                  case e: NativeErrorException if e.returnCode == 111 =>
                    logger.debug(s"Error when connecting to $socketFile: ${e.getMessage}")
                    null
                }
            }
            count += 1
          }
          if (socket != null) {
            logger.debug(s"BSP connection at $socketFile opened")
            socket
          }
          else if (closed.value.isEmpty)
            sys.error(s"Timeout while waiting for BSP socket to be created in $socketFile")
          else
            sys.error(
              s"Bloop BSP connection in $socketFile was unexpectedly closed or bloop didn't start."
            )
      }
      val closed = promise.future
      def stop() = stop0.set(true)
    }
  }

}
