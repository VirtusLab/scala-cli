package scala.build.bloop.bloopgun

import java.io.{FileOutputStream, InputStream, OutputStream}
import java.nio.file.Path
import java.util.concurrent.ScheduledExecutorService

import scala.build.bloop.bloopgun.internal.{Operations, Util}
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.control.NonFatal

object Bloopgun {

  /**
    * Checks whether a bloop server is running at this host / port.
    *
    * @param host
    * @param port
    * @param logger
    * @return Whether a server is running or not.
    */
  def check(
    config: BloopgunConfig,
    logger: BloopgunLogger
  ): Boolean =
    Operations.check(
      config.host,
      config.port,
      logger
    )

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
    config: BloopgunConfig,
    scheduler: ScheduledExecutorService,
    waitInterval: FiniteDuration,
    timeout: Duration,
    logger: BloopgunLogger
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

  /**
    * Opens a BSP connection to a running bloop server.
    *
    * Starts a thread to read output from the nailgun connection, and another one
    * to pass input to it.
    *
    * @param logger
    * @return A [[BspConnection]] object, that can be used to close the connection.
    */
  def bsp(
    config: BloopgunConfig,
    workingDir: Path,
    logger: BloopgunLogger
  ): BspConnection = {

    val bspPort = config.bspPort.getOrElse {
      Util.randomPort()
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
        bspPort,
        workingDir,
        in,
        out,
        err,
        logger
      )

      new BspConnection {
        def address = conn.address
        def openSocket() = conn.openSocket()
        def closed = conn.closed
        def stop(): Unit = {
          if (devNullOs != null)
            devNullOs.close()
          conn.stop()
        }
      }
    } catch {
      case NonFatal(e) =>
        if (devNullOs != null)
          devNullOs.close()
        throw e
    }
  }

}
