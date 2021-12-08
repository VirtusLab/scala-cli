package scala.build.blooprifle.internal

import org.scalasbt.ipcsocket.NativeErrorException
import snailgun.TcpClient
import snailgun.protocol.Streams

import java.io.{File, IOException, InputStream, OutputStream}
import java.net.{ConnectException, InetSocketAddress, Socket}
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ExecutorService, ScheduledExecutorService, ScheduledFuture}

import scala.build.blooprifle.{BloopRifleLogger, BspConnection, BspConnectionAddress}
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success, Try}

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

  private val ignoredErrnos = Set(
    61, // Connection refused
    111 //
  )

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
    bspSocketOrPort: BspConnectionAddress,
    workingDir: Path,
    in: InputStream,
    out: OutputStream,
    err: OutputStream,
    logger: BloopRifleLogger
  ): BspConnection = {

    val stop0         = new AtomicBoolean
    val nailgunClient = TcpClient(host, port)
    val streams       = Streams(in, out, err)

    val promise    = Promise[Int]()
    val threadName = "bloop-rifle-nailgun-out"
    val protocolArgs = bspSocketOrPort match {
      case t: BspConnectionAddress.Tcp =>
        Array("--protocol", "tcp", "--host", host, "--port", t.port.toString)
      case s: BspConnectionAddress.UnixDomainSocket =>
        Array("--protocol", "local", "--socket", s.path.getAbsolutePath)
      case p: BspConnectionAddress.WindowsNamedPipe =>
        Array("--protocol", "local", "--pipe-name", p.name)
    }
    val runnable: Runnable = logger.runnable(threadName) { () =>
      val maybeRetCode = Try {
        nailgunClient.run(
          "bsp",
          protocolArgs,
          workingDir,
          sys.env.toMap,
          streams,
          logger.nailgunLogger,
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
        case t: BspConnectionAddress.Tcp => s"$host:${t.port}"
        case s: BspConnectionAddress.UnixDomainSocket =>
          "local:" + s.path.toURI.toASCIIString.stripPrefix("file:")
        case p: BspConnectionAddress.WindowsNamedPipe => p.name
      }
      def openSocket(period: FiniteDuration, timeout: FiniteDuration) = bspSocketOrPort match {
        case t: BspConnectionAddress.Tcp =>
          new Socket(host, t.port)
        case s: BspConnectionAddress.UnixDomainSocket =>
          val socketFile     = s.path
          var count          = 0
          val maxCount       = (timeout / period).toInt
          var socket: Socket = null
          while (socket == null && count < maxCount && closed.value.isEmpty) {
            logger.debug {
              if (socketFile.exists())
                s"BSP connection $socketFile found but not open, waiting $period"
              else
                s"BSP connection at $socketFile not found, waiting $period"
            }
            Thread.sleep(period.toMillis)
            if (socketFile.exists())
              socket =
                // format: off
                try {
                  try {
                    (new NamedSocketBuilder).create(socketFile.getAbsolutePath)
                  }
                // format: on
                  catch {
                    case ex: RuntimeException if ex.getMessage == "NamedSocketBuilder" =>
                      throw ex.getCause
                  }
                }
                catch {
                  case ExCause(ex0: NativeErrorException) if ignoredErrnos(ex0.returnCode) =>
                    logger.debug(s"Error when connecting to $socketFile: ${ex0.getMessage}")
                    null
                  case e: NativeErrorException if ignoredErrnos(e.returnCode) =>
                    logger.debug(s"Error when connecting to $socketFile: ${e.getMessage}")
                    null
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
        case p: BspConnectionAddress.WindowsNamedPipe =>
          var count          = 0
          val maxCount       = (timeout / period).toInt
          var socket: Socket = null
          while (socket == null && count < maxCount && closed.value.isEmpty) {
            Thread.sleep(period.toMillis)
            socket =
              // format: off
              try {
                try {
                  (new NamedSocketBuilder).create(p.name)
                }
              // format: on
                catch {
                  case ex: RuntimeException if ex.getMessage == "NamedSocketBuilder" =>
                    throw ex.getCause
                }
              }
              catch {
                case ex: IOException
                    if ex.getMessage != null &&
                      ex.getMessage.contains("The system cannot find the file specified.") =>
                  logger.debug(s"Error when connecting to ${p.name}: ${ex.getMessage}")
                  null
                case e: NativeErrorException if e.returnCode == 111 =>
                  logger.debug(s"Error when connecting to ${p.name}: ${e.getMessage}")
                  null
              }
            count += 1
          }
          if (socket != null) {
            logger.debug(s"BSP connection at ${p.name} opened")
            socket
          }
          else if (closed.value.isEmpty)
            sys.error(s"Timeout while waiting for BSP socket to be created in ${p.name}")
          else
            sys.error(
              s"Bloop BSP connection in ${p.name} was unexpectedly closed or bloop didn't start."
            )
      }
      val closed = promise.future
      def stop() = stop0.set(true)
    }
  }

  def exit(
    host: String,
    port: Int,
    workingDir: Path,
    in: InputStream,
    out: OutputStream,
    err: OutputStream,
    logger: BloopRifleLogger
  ): Int = {

    val stop0         = new AtomicBoolean
    val nailgunClient = TcpClient(host, port)
    val streams       = Streams(in, out, err)

    nailgunClient.run(
      "exit",
      Array.empty,
      workingDir,
      sys.env.toMap,
      streams,
      logger.nailgunLogger,
      stop0,
      interactiveSession = false
    )
  }

  def about(
    host: String,
    port: Int,
    workingDir: Path,
    in: InputStream,
    out: OutputStream,
    err: OutputStream,
    logger: BloopRifleLogger,
    scheduler: ExecutorService
  ): Int = {

    val stop0         = new AtomicBoolean
    val nailgunClient = TcpClient(host, port)
    val streams       = Streams(in, out, err)

    timeout(30.seconds, scheduler, logger) {
      nailgunClient.run(
        "about",
        Array.empty,
        workingDir,
        sys.env.toMap,
        streams,
        logger.nailgunLogger,
        stop0,
        interactiveSession = false
      )
    }

  }

  def timeout[T](
    duration: Duration,
    scheduler: ExecutorService,
    logger: BloopRifleLogger
  )(body: => T) = {
    val p = Promise[T]()
    scheduler.execute { () =>
      try {
        val retCode = body
        p.tryComplete(Success(retCode))
      }
      catch {
        case t: Throwable =>
          logger.debug(s"Caught $t while trying to run code with timeout")
          p.tryComplete(Failure(t))
      }
    }

    Await.result(p.future, duration)
  }

  private object ExCause {
    def unapply(ex: Throwable): Option[Throwable] =
      Option(ex.getCause)
  }
}
