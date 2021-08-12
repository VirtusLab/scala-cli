package scala.cli.commands

import caseapp._

import java.io.File
import java.nio.file.{AtomicMoveNotSupportedException, FileAlreadyExistsException, Files}
import java.util.Random

import scala.build.Os
import scala.cli.internal.Pid
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Properties

final case class SharedCompilationServerOptions(
  @Group("Compilation server")
  @HelpMessage("Protocol to use to open a BSP connection with Bloop (ignored on Windows for now)")
  @ValueDescription("tcp|local|default")
  @Hidden
    bloopBspProtocol: Option[String] = None,
  @Group("Compilation server")
  @HelpMessage("Socket file to use to open a BSP connection with Bloop (ignored on Windows for now)")
  @ValueDescription("path")
  @Hidden
    bloopBspSocket: Option[String] = None,

  @Group("Compilation server")
  @HelpMessage("Host the compilation server should bind to")
  @ValueDescription("host")
  @Hidden
    bloopHost: Option[String] = None,
  @Group("Compilation server")
  @HelpMessage("Port the compilation server should bind to (pass -1 to pick a random port)")
  @ValueDescription("port|-1")
  @Hidden
    bloopPort: Option[Int] = None,

  @Hidden
  @Group("Compilation server")
  @HelpMessage("Maximum duration to wait for BSP connection to be opened")
  @ValueDescription("duration")
    bloopBspTimeout: Option[String] = None,
  @Hidden
  @Group("Compilation server")
  @HelpMessage("Duration between checks of the BSP connection state")
  @ValueDescription("duration")
    bloopBspCheckPeriod: Option[String] = None,
  @Hidden
  @Group("Compilation server")
  @HelpMessage("Maximum duration to wait for compilation server to start up")
  @ValueDescription("duration")
    bloopStartupTimeout: Option[String] = None
) {

  private lazy val pidOrRandom: Either[Int, Int] =
    Option((new Pid).get()).map(_.intValue()).map(Right(_)).getOrElse {
      val r = new Random
      Left(r.nextInt())
    }
  private def socketDirectory(directories: scala.build.Directories): os.Path = {
    val dir = directories.bspSocketDir
    // Ensuring that whenever dir exists, it has the right permissions
    if (!os.isDir(dir)) {
      val tmpDir = dir / os.up / s".${dir.last}.tmp-${pidOrRandom.merge}"
      try {
        os.makeDir.all(tmpDir)
        os.perms.set(tmpDir, "rwx------")
        try os.move(tmpDir, dir, atomicMove = true)
        catch {
          case _: AtomicMoveNotSupportedException =>
            try os.move(tmpDir, dir)
            catch {
              case _: FileAlreadyExistsException =>
            }
          case _: FileAlreadyExistsException =>
        }
      } finally {
        if (os.exists(tmpDir))
          os.remove(tmpDir)
      }
    }
    dir
  }

  private def bspSocketFile(directories: => scala.build.Directories): File = {
    val (socket, deleteOnExit) = bloopBspSocket match {
      case Some(path) =>
        (os.Path(path, Os.pwd), false)
      case None =>
        val dir = socketDirectory(directories)
        val fileName = pidOrRandom
          .map("proc-" + _)
          .left.map("conn-" + _)
          .merge
        (dir / fileName, true)
    }
    if (deleteOnExit)
      Runtime.getRuntime.addShutdownHook(
        new Thread("delete-bloop-bsp-named-socket") {
          override def run() =
            Files.deleteIfExists(socket.toNIO)
        }
      )
    socket.toIO.getCanonicalFile
  }

  def defaultBspSocketOrPort(directories: => scala.build.Directories): Option[() => Either[Int, File]] =
    if (Properties.isWin) None
    else
      bloopBspProtocol.filter(_ != "default") match {
        case None => None
        case Some("tcp") => None
        case Some("local") =>
          Some(() => Right(bspSocketFile(directories)))
        case Some(other) =>
          sys.error(s"Invalid bloop BSP protocol value: '$other' (expected 'tcp', 'local', or 'default')")
      }

  private def parseDuration(name: String, valueOpt: Option[String]): Option[FiniteDuration] =
    valueOpt.map(_.trim).filter(_.nonEmpty).map(Duration(_)).map {
      case d: FiniteDuration => d
      case d => sys.error(s"Expected finite $name duration, got $d")
    }

  def bloopBspTimeoutDuration: Option[FiniteDuration] =
    parseDuration("BSP connection timeout", bloopBspTimeout)
  def bloopBspCheckPeriodDuration: Option[FiniteDuration] =
    parseDuration("BSP connection check period", bloopBspCheckPeriod)
  def bloopStartupTimeoutDuration: Option[FiniteDuration] =
    parseDuration("connection server startup timeout", bloopStartupTimeout)

}
