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
    logger: BloopRifleLogger,
    scheduler: ScheduledExecutorService
  ): Boolean = {
    def check() =
      Operations.check(
        config.host,
        config.port,
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
          config.host,
          config.port,
          bloopJava,
          config.javaOpts,
          cp.map(_.toPath),
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
      BspConnectionAddress.Tcp(Util.randomPort())
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
        config.host,
        config.port,
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
        )          = conn.openSocket(period, timeout)
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
        config.host,
        config.port,
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

  // Probably we should implement an endpoint in Bloop to get this
  // information in better form. I'm not sure error here should be escalated or ignored.
  private def extractVersionFromBloopAbout(stdoutFromBloopAbout: String)
    : Option[BloopServerRuntimeInfo] = {

    val bloopVersionRegex = "bloop v(.*)\\s".r
    val bloopJvmRegex     = "Running on Java JDK v([0-9._A-Za-z]+) [(](.*)[)]".r

    for {
      bloopVersion    <- bloopVersionRegex.findFirstMatchIn(stdoutFromBloopAbout).map(_.group(1))
      bloopJvmVersion <- bloopJvmRegex.findFirstMatchIn(stdoutFromBloopAbout).map(_.group(1))
      javaHome        <- bloopJvmRegex.findFirstMatchIn(stdoutFromBloopAbout).map(_.group(2))
      jvmRelease      <- VersionUtil.jvmRelease(bloopJvmVersion)
    } yield BloopServerRuntimeInfo(
      bloopVersion = BloopVersion(bloopVersion),
      jvmVersion = jvmRelease,
      javaHome = javaHome
    )
  }

  def getCurrentBloopVersion(
    config: BloopRifleConfig,
    logger: BloopRifleLogger,
    workdir: Path,
    scheduler: ScheduledExecutorService
  ): Either[BloopAboutFailure, BloopServerRuntimeInfo] = {
    val isRunning = BloopRifle.check(config, logger, scheduler)

    if (isRunning) {
      val bufferedOStream = new ByteArrayOutputStream(100000)
      Operations.about(
        config.host,
        config.port,
        workdir,
        nullInputStream(),
        bufferedOStream,
        nullOutputStream(),
        logger,
        scheduler
      )
      val bloopAboutOutput = new String(bufferedOStream.toByteArray)
      extractVersionFromBloopAbout(bloopAboutOutput) match {
        case Some(value) => Right(value)
        case None        => Left(ParsingFailed(bloopAboutOutput))
      }
    }
    else
      Left(BloopNotRunning)
  }
}

sealed trait BloopAboutFailure
case object BloopNotRunning                        extends BloopAboutFailure
case class ParsingFailed(bloopAboutOutput: String) extends BloopAboutFailure

case class BloopServerRuntimeInfo(
  bloopVersion: BloopVersion,
  jvmVersion: Int,
  javaHome: String
)
