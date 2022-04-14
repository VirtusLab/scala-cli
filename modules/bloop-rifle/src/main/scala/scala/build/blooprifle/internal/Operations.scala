package scala.build.blooprifle.internal

import libdaemonjvm.LockFiles
import snailgun.protocol.Streams
import snailgun.{Client, TcpClient}

import java.io.{File, InputStream, OutputStream}
import java.net.{ConnectException, InetSocketAddress, Socket, StandardProtocolFamily, UnixDomainSocketAddress}
import java.nio.channels.SocketChannel
import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.{Files, Path}
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ExecutorService, ScheduledExecutorService, ScheduledFuture}

import scala.build.blooprifle.{BloopRifleConfig, BloopRifleLogger, BspConnection, BspConnectionAddress}
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Properties, Success, Try}

object Operations {

  private def lockFiles(address: BloopRifleConfig.Address.DomainSocket): LockFiles = {
    val path = address.path
    if (!Files.exists(path)) {
      // FIXME Small change of race condition here between createDirectories and setPosixFilePermissions
      Files.createDirectories(path)
      if(!Properties.isWin) Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwx------"))
    }
    LockFiles.under(path)
  }

  /** Checks whether a bloop server is running at this host / port.
    *
    * @param host
    * @param port
    * @param logger
    * @return
    *   Whether a server is running or not.
    */
  def check(
    address: BloopRifleConfig.Address,
    logger: BloopRifleLogger
  ): Boolean = {
    logger.debug(s"Checking for a running Bloop server at ${address.render} ...")
    address match {
      case BloopRifleConfig.Address.Tcp(host, port) =>
        // inspired by https://github.com/scalacenter/bloop/blob/cbddb8baaf639a4e08ee630f1ebc559dc70255a8/bloopgun/src/main/scala/bloop/bloopgun/core/Shell.scala#L174-L202
        Util.withSocket { socket =>
          socket.setReuseAddress(true)
          socket.setTcpNoDelay(true)
          logger.debug(s"Attempting to connect to Bloop server ${address.render} ...")
          val res =
            try {
              socket.connect(new InetSocketAddress(host, port))
              socket.isConnected()
            }
            catch {
              case _: ConnectException => false
            }
          logger.debug(s"Connection attempt result: $res")
          res
        }
      case addr: BloopRifleConfig.Address.DomainSocket =>
        val files = lockFiles(addr)
        logger.debug(s"Attempting to connect to Bloop server ${address.render} ...")
        val res = libdaemonjvm.client.Connect.tryConnect(files)
        logger.debug(s"Connection attempt result: $res")
        res match {
          case Some(Right(e)) => e.close()
          case _              =>
        }
        res.exists(_.isRight)
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
    address: BloopRifleConfig.Address,
    javaPath: String,
    javaOpts: Seq[String],
    classPath: Seq[Path],
    workingDir: File,
    scheduler: ScheduledExecutorService,
    waitInterval: FiniteDuration,
    timeout: Duration,
    logger: BloopRifleLogger,
    bloopServerSupportsFileTruncating: Boolean
  ): Future[Unit] = {

    val (addressArgs, mainClass, writeOutputToOpt) = address match {
      case BloopRifleConfig.Address.Tcp(host, port) =>
        (Seq(host, port.toString), "bloop.Server", None)
      case s: BloopRifleConfig.Address.DomainSocket =>
        val writeOutputToOpt0 =
          if (bloopServerSupportsFileTruncating) Some(s.outputPath)
          else None
        (Seq(s"daemon:${s.path}"), "bloop.Bloop", writeOutputToOpt0)
    }

    val extraJavaOpts =
      writeOutputToOpt.toSeq.map { writeOutputTo =>
        s"-Dbloop.truncate-output-file-periodically=${writeOutputTo.toAbsolutePath}"
      }

    val command =
      Seq(javaPath) ++
        extraJavaOpts ++
        javaOpts ++
        Seq(
          "-cp",
          classPath.map(_.toString).mkString(File.pathSeparator),
          mainClass
        ) ++
        addressArgs
    val b = new ProcessBuilder(command: _*)
    b.directory(workingDir)
    b.redirectInput(ProcessBuilder.Redirect.PIPE)

    if (logger.bloopCliInheritStdout)
      b.redirectOutput(ProcessBuilder.Redirect.INHERIT)
    else
      writeOutputToOpt match {
        case Some(writeOutputTo) =>
          b.redirectOutput(writeOutputTo.toFile)
        case None =>
          b.redirectOutput(ProcessBuilder.Redirect.DISCARD)
      }

    if (logger.bloopCliInheritStderr)
      b.redirectError(ProcessBuilder.Redirect.INHERIT)
    else
      writeOutputToOpt match {
        case Some(writeOutputTo) =>
          b.redirectError(writeOutputTo.toFile)
        case None =>
          b.redirectError(ProcessBuilder.Redirect.DISCARD)
      }

    val p = b.start()
    p.getOutputStream.close()

    val promise = Promise[Unit]()

    def check0(f: => ScheduledFuture[_]): Runnable = {
      val start = System.currentTimeMillis()
      () =>
        try {
          val completionOpt =
            if (!p.isAlive())
              Some(Failure(new Exception("Server didn't start")))
            else if (check(address, logger))
              Some(Success(()))
            else if (timeout.isFinite && System.currentTimeMillis() - start > timeout.toMillis)
              Some(Failure(new Exception(s"Server didn't start after $timeout ms")))
            else
              None

          for (completion <- completionOpt) {
            promise.tryComplete(completion)
            f.cancel(false)
          }
        }
        catch {
          case t: Throwable =>
            if (timeout.isFinite && System.currentTimeMillis() - start > timeout.toMillis) {
              promise.tryFailure(t)
              f.cancel(false)
            }
            throw t
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

  private def nailgunClient(address: BloopRifleConfig.Address): Client =
    address match {
      case BloopRifleConfig.Address.Tcp(host, port) =>
        TcpClient(host, port)
      case addr: BloopRifleConfig.Address.DomainSocket =>
        SnailgunClient { () =>
          val files = lockFiles(addr)
          val res   = libdaemonjvm.client.Connect.tryConnect(files)
          res match {
            case None          => ??? // not running
            case Some(Left(_)) => ??? // error
            case Some(Right(channel)) =>
              libdaemonjvm.Util.socketFromChannel(channel)
          }
        }
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
    address: BloopRifleConfig.Address,
    bspSocketOrPort: BspConnectionAddress,
    workingDir: Path,
    in: InputStream,
    out: OutputStream,
    err: OutputStream,
    logger: BloopRifleLogger
  ): BspConnection = {

    val stop0          = new AtomicBoolean
    val nailgunClient0 = nailgunClient(address)
    val streams        = Streams(in, out, err)

    val promise    = Promise[Int]()
    val threadName = "bloop-rifle-nailgun-out"
    val protocolArgs = bspSocketOrPort match {
      case t: BspConnectionAddress.Tcp =>
        Array("--protocol", "tcp", "--host", t.host, "--port", t.port.toString)
      case s: BspConnectionAddress.UnixDomainSocket =>
        Array("--protocol", "local", "--socket", s.path.getAbsolutePath)
    }
    val runnable: Runnable = logger.runnable(threadName) { () =>
      val maybeRetCode = Try {
        nailgunClient0.run(
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
        case t: BspConnectionAddress.Tcp => s"${t.host}:${t.port}"
        case s: BspConnectionAddress.UnixDomainSocket =>
          "local:" + s.path.toURI.toASCIIString.stripPrefix("file:")
      }
      def openSocket(period: FiniteDuration, timeout: FiniteDuration) = bspSocketOrPort match {
        case t: BspConnectionAddress.Tcp =>
          new Socket(t.host, t.port)
        case s: BspConnectionAddress.UnixDomainSocket =>
          val socketFile            = s.path
          var count                 = 0
          val maxCount              = (timeout / period).toInt
          var socket: SocketChannel = null
          while (socket == null && count < maxCount && closed.value.isEmpty) {
            logger.debug {
              if (socketFile.exists())
                s"BSP connection $socketFile found but not open, waiting $period"
              else
                s"BSP connection at $socketFile not found, waiting $period"
            }
            Thread.sleep(period.toMillis)
            if (socketFile.exists()) {
              val addr = UnixDomainSocketAddress.of(socketFile.toPath)
              socket = SocketChannel.open(StandardProtocolFamily.UNIX)
              socket.connect(addr)
              socket.finishConnect()
            }
            count += 1
          }
          if (socket != null) {
            logger.debug(s"BSP connection at $socketFile opened")
            libdaemonjvm.Util.socketFromChannel(socket)
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

  def exit(
    address: BloopRifleConfig.Address,
    workingDir: Path,
    in: InputStream,
    out: OutputStream,
    err: OutputStream,
    logger: BloopRifleLogger
  ): Int = {

    val stop0          = new AtomicBoolean
    val nailgunClient0 = nailgunClient(address)
    val streams        = Streams(in, out, err)

    nailgunClient0.run(
      "ng-stop",
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
    address: BloopRifleConfig.Address,
    workingDir: Path,
    in: InputStream,
    out: OutputStream,
    err: OutputStream,
    logger: BloopRifleLogger,
    scheduler: ExecutorService
  ): Int = {

    val stop0          = new AtomicBoolean
    val nailgunClient0 = nailgunClient(address)
    val streams        = Streams(in, out, err)

    timeout(30.seconds, scheduler, logger) {
      nailgunClient0.run(
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
}
