package scala.build.blooprifle

import java.io.{
  ByteArrayOutputStream,
  File,
  FileInputStream,
  FileOutputStream,
  InputStream,
  OutputStream
}
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
    def check() = {
      Operations.check(
        config.host,
        config.port,
        logger
      )
    }
    check() && {
      !BloopRifle.shutdownBloopIfVersionIncompatible(
        config,
        logger,
        new File(".").getCanonicalFile.toPath,
        scheduler
      )
    }
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
    logger: BloopRifleLogger
  ): Future[Unit] =
    config.classPath() match {
      case Left(ex) => Future.failed(new Exception("Error getting Bloop class path", ex))
      case Right(cp) =>
        Operations.startServer(
          config.host,
          config.port,
          config.javaPath,
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

  def extractVersionFromBloopAbout(stdoutFromBloopAbout: String): Option[String] = {
    stdoutFromBloopAbout.split("\n").find(_.startsWith("bloop v")).map(
      _.split(" ")(1).trim().drop(1)
    )
  }

  def getCurrentBloopVersion(
    config: BloopRifleConfig,
    logger: BloopRifleLogger,
    workdir: Path,
    scheduler: ScheduledExecutorService
  ) = {
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
    extractVersionFromBloopAbout(new String(bufferedOStream.toByteArray))
  }

  /** Sometimes we need some minimal requirements for Bloop version. This method kills Bloop if its
    * version is unsupported.
    * @returns
    *   true if the 'exit' command has actually been sent to Bloop
    */
  def shutdownBloopIfVersionIncompatible(
    config: BloopRifleConfig,
    logger: BloopRifleLogger,
    workdir: Path,
    scheduler: ScheduledExecutorService
  ): Boolean = {
    val currentBloopVersion = getCurrentBloopVersion(config, logger, workdir, scheduler)
    val bloopExitNeeded = config.acceptBloopVersion.exists { f =>
      currentBloopVersion.map(f).getOrElse(true)
    }
    if (bloopExitNeeded) {
      logger.debug(
        s"Shutting down unsupported bloop v${currentBloopVersion}."
      )
      val retCode = exit(config, workdir, logger)
      logger.debug(s"Bloop exit return code: $retCode")
    }
    else {
      logger.debug("No need to reset bloop")
    }
    bloopExitNeeded
  }
}
