package scala.build.blooprifle

import java.io.{ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.file.Path
import java.util.concurrent.ScheduledExecutorService

import scala.build.blooprifle.internal.{Operations, Util}
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

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
      case Right((cp, isScalaCliBloop)) =>
        object IntValue {
          def unapply(s: String): Option[Int] =
            // no String.toIntOption in Scala 2.12.x
            try Some(s.toInt)
            catch {
              case _: NumberFormatException => None
            }
        }
        val bloopServerSupportsFileTruncating =
          isScalaCliBloop && {
            version.takeWhile(c => c.isDigit || c == '.').split('.') match {
              case Array(IntValue(maj), IntValue(min), IntValue(patch)) =>
                import scala.math.Ordering.Implicits._
                Seq(maj, min, patch) >= Seq(1, 14, 20)
              case _ =>
                false
            }
          }
        Operations.startServer(
          config.address,
          bloopJava,
          config.javaOpts,
          cp.map(_.toPath),
          config.workingDir,
          scheduler,
          config.startCheckPeriod,
          config.startCheckTimeout,
          logger,
          bloopServerSupportsFileTruncating = bloopServerSupportsFileTruncating
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

    val out = config.bspStdout.getOrElse(OutputStream.nullOutputStream())
    val err = config.bspStderr.getOrElse(OutputStream.nullOutputStream())

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
      def stop(): Unit =
        conn.stop()
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

    val out = config.bspStdout.getOrElse(OutputStream.nullOutputStream())
    val err = config.bspStderr.getOrElse(OutputStream.nullOutputStream())

    Operations.exit(
      config.address,
      workingDir,
      in,
      out,
      err,
      logger
    )
  }

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
        InputStream.nullInputStream(),
        bufferedOStream,
        OutputStream.nullOutputStream(),
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
