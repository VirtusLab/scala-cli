package scala.build.blooprifle

import java.io.{FileOutputStream, InputStream, OutputStream}
import java.nio.file.{Files, Path}
import java.util.concurrent.ScheduledExecutorService

import scala.build.blooprifle.internal.{Operations, Util}
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.control.NonFatal
import scala.util.Properties

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
  ): Boolean =
    Operations.check(
      config.host,
      config.port,
      logger
    )

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
    config: BloopRifleConfig,
    scheduler: ScheduledExecutorService,
    waitInterval: FiniteDuration,
    timeout: Duration,
    logger: BloopRifleLogger
  ): Future[Unit] = {

    val classPath = config.classPath().map(_.toPath)

    Operations.startServer(
      config.host,
      config.port,
      config.javaPath,
      config.javaOpts,
      classPath,
      scheduler,
      waitInterval,
      timeout,
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
      Left(Util.randomPort())
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
        def address      = conn.address
        def openSocket() = conn.openSocket()
        def closed       = conn.closed
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

}
