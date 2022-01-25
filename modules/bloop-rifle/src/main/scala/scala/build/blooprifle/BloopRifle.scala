package scala.build.blooprifle

import java.io.{ByteArrayOutputStream, FileInputStream, FileOutputStream, InputStream, OutputStream}
import java.nio.file.Path
import java.util.concurrent.ScheduledExecutorService

import scala.build.blooprifle.internal.{Operations, Util}
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal

object BloopRifle {

  /** Checks whether a bloop server is running at this host / port.
    *
    * @param host
    * @param port
    * @param logger
    * @return
    *   Whether a server is running or not.
    */
  def check(
    config: BloopRifleConfig,
    logger: BloopRifleLogger
  ): Boolean = {
    def check() =
      Operations.check(
        config.address,
        logger
      )
    check()
  }

  /** Starts a new bloop server.
    *
    * @param config
    * @param scheduler
    * @param logger
    * @return
    *   A future, that gets completed when the server is done starting (and can thus be used).
    */
  def startServer(
    config: BloopRifleConfig,
    scheduler: ScheduledExecutorService,
    logger: BloopRifleLogger,
    version: String,
    bloopJava: String
  ): Future[Unit] =
    config.classPath(version) match {
      case Left(ex) => Future.failed(new Exception("Error getting Bloop class path", ex))
      case Right(cp) =>
        Operations.startServer(
          config.address,
          bloopJava,
          config.javaOpts,
          cp.map(_.toPath),
          config.workingDir,
          scheduler,
          config.startCheckPeriod,
          config.startCheckTimeout,
          logger
        )
    }

  /** Opens a BSP connection to a running bloop server.
    *
    * Starts a thread to read output from the nailgun connection, and another one to pass input to
    * it.
    *
    * @param logger
    * @return
    *   A [[BspConnection]] object, that can be used to close the connection.
    */
  def bsp(
    config: BloopRifleConfig,
    workingDir: Path,
    logger: BloopRifleLogger
  ): BspConnection = {

    val bspSocketOrPort = config.bspSocketOrPort.map(_()).getOrElse {
      BspConnectionAddress.Tcp("127.0.0.1", Util.randomPort())
    }

    val in = config.bspStdin.getOrElse {
      new InputStream {
        def read(): Int = -1
      }
    }
    var devNullOs: OutputStream = null
    def devNull(): OutputStream = {
      if (devNullOs == null)
        devNullOs = new FileOutputStream(Util.devNull)
      devNullOs
    }

    try {
      val out = config.bspStdout.getOrElse(devNull())
      val err = config.bspStderr.getOrElse(devNull())

      val conn = Operations.bsp(
        config.address,
        bspSocketOrPort,
        workingDir,
        in,
        out,
        err,
        logger
      )

      new BspConnection {
        def address = conn.address
        def openSocket(
          period: FiniteDuration,
          timeout: FiniteDuration
        ) = conn.openSocket(period, timeout)
        def closed = conn.closed
        def stop(): Unit = {
          if (devNullOs != null)
            devNullOs.close()
          conn.stop()
        }
      }
    }
    catch {
      case NonFatal(e) =>
        if (devNullOs != null)
          devNullOs.close()
        throw e
    }
  }

  def exit(
    config: BloopRifleConfig,
    workingDir: Path,
    logger: BloopRifleLogger
  ): Int = {

    val in = config.bspStdin.getOrElse {
      new InputStream {
        def read(): Int = -1
      }
    }
    var devNullOs: OutputStream = null
    def devNull(): OutputStream = {
      if (devNullOs == null)
        devNullOs = new FileOutputStream(Util.devNull)
      devNullOs
    }

    try {
      val out = config.bspStdout.getOrElse(devNull())
      val err = config.bspStderr.getOrElse(devNull())

      Operations.exit(
        config.address,
        workingDir,
        in,
        out,
        err,
        logger
      )
    }
    catch {
      case NonFatal(e) =>
        if (devNullOs != null)
          devNullOs.close()
        throw e
    }
  }

  def nullOutputStream() = new FileOutputStream(Util.devNull)

  def nullInputStream() = new FileInputStream(Util.devNull)

  def getCurrentBloopVersion(
    config: BloopRifleConfig,
    logger: BloopRifleLogger,
    workdir: Path,
    scheduler: ScheduledExecutorService
  ): Either[BloopAboutFailure, BloopServerRuntimeInfo] = {
    val isRunning = BloopRifle.check(config, logger)

    if (isRunning) {
      val bufferedOStream = new ByteArrayOutputStream(100000)
      Operations.about(
        config.address,
        workdir,
        nullInputStream(),
        bufferedOStream,
        nullOutputStream(),
        logger,
        scheduler
      )
      val bloopAboutOutput = new String(bufferedOStream.toByteArray)
      VersionUtil.parseBloopAbout(bloopAboutOutput) match {
        case Some(value) => Right(value)
        case None        => Left(ParsingFailed(bloopAboutOutput))
      }
    }
    else
      Left(BloopNotRunning)
  }
}

sealed abstract class BloopAboutFailure extends Product with Serializable {
  def message: String
}
case object BloopNotRunning extends BloopAboutFailure {
  def message = "not running"
}
case class ParsingFailed(bloopAboutOutput: String) extends BloopAboutFailure {
  def message = s"failed to parse output: '$bloopAboutOutput'"
}

case class BloopServerRuntimeInfo(
  bloopVersion: BloopVersion,
  jvmVersion: Int,
  javaHome: String
) {
  def message: String =
    s"version $bloopVersion, JVM $jvmVersion under $javaHome"
}
