package scala.cli.commands.shared

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import coursier.cache.FileCache
import coursier.core.{Version => Ver}
import coursier.util.Task

import java.io.File
import java.nio.file.{AtomicMoveNotSupportedException, FileAlreadyExistsException, Files, Paths}
import java.util.Random

import scala.build.blooprifle.internal.Constants
import scala.build.blooprifle.{BloopRifleConfig, BloopVersion, BspConnectionAddress}
import scala.build.internal.Util
import scala.build.{Bloop, Logger, Os}
import scala.cli.commands.bloop.BloopJson
import scala.cli.commands.shared.*
import scala.cli.internal.Pid
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Properties

// format: off
final case class SharedCompilationServerOptions(
  @Group(HelpGroup.CompilationServer.toString)
  @HelpMessage("Protocol to use to open a BSP connection with Bloop")
  @ValueDescription("tcp|local|default")
  @Hidden
    bloopBspProtocol: Option[String] = None,
  @Group(HelpGroup.CompilationServer.toString)
  @HelpMessage("Socket file to use to open a BSP connection with Bloop")
  @ValueDescription("path")
  @Hidden
    bloopBspSocket: Option[String] = None,

  @Group(HelpGroup.CompilationServer.toString)
  @HelpMessage("Host the compilation server should bind to")
  @ValueDescription("host")
  @Hidden
    bloopHost: Option[String] = None,
  @Group(HelpGroup.CompilationServer.toString)
  @HelpMessage("Port the compilation server should bind to (pass `-1` to pick a random port)")
  @ValueDescription("port|-1")
  @Hidden
    bloopPort: Option[Int] = None,
  @Group(HelpGroup.CompilationServer.toString)
  @HelpMessage("Daemon directory of the Bloop daemon (directory with lock, pid, and socket files)")
  @ValueDescription("path")
  @Hidden
    bloopDaemonDir: Option[String] = None,

  @Group(HelpGroup.CompilationServer.toString)
  @HelpMessage("If Bloop isn't already running, the version we should start")
  @ValueDescription("version")
  @Hidden
    bloopVersion: Option[String] = None,

  @Hidden
  @Group(HelpGroup.CompilationServer.toString)
  @HelpMessage("Maximum duration to wait for the BSP connection to be opened")
  @ValueDescription("duration")
    bloopBspTimeout: Option[String] = None,
  @Hidden
  @Group(HelpGroup.CompilationServer.toString)
  @HelpMessage("Duration between checks of the BSP connection state")
  @ValueDescription("duration")
    bloopBspCheckPeriod: Option[String] = None,
  @Hidden
  @Group(HelpGroup.CompilationServer.toString)
  @HelpMessage("Maximum duration to wait for the compilation server to start up")
  @ValueDescription("duration")
    bloopStartupTimeout: Option[String] = None,

  @Group(HelpGroup.CompilationServer.toString)
  @HelpMessage("Include default JVM options for Bloop")
  @Hidden
    bloopDefaultJavaOpts: Boolean = true,
  @Group(HelpGroup.CompilationServer.toString)
  @HelpMessage("Pass java options to use by Bloop server")
  @Hidden
    bloopJavaOpt: List[String] = Nil,
  @Group(HelpGroup.CompilationServer.toString)
  @HelpMessage("Bloop global options file")
  @Hidden
    bloopGlobalOptionsFile: Option[String] = None,

  @Group(HelpGroup.CompilationServer.toString)
  @HelpMessage("JVM to use to start Bloop (e.g. 'system|11', 'temurin:17', …)")
  @Hidden
    bloopJvm: Option[String] = None,

  @Group(HelpGroup.CompilationServer.toString)
  @HelpMessage("Working directory for Bloop, if it needs to be started")
  @Hidden
    bloopWorkingDir: Option[String] = None,

  @Group(HelpGroup.CompilationServer.toString)
  @HelpMessage("Enable / disable usage of Bloop compilation server. Bloop is used by default so use `--server=false` to disable it. Disabling compilation server allows to test compilation in more controlled mannter (no caching or incremental compiler) but has a detrimental effect of performance.")
    server: Option[Boolean] = None
) {
  // format: on

  private def pidOrRandom: Either[Int, Int] =
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
        if (!Properties.isWin)
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
      }
      finally if (os.exists(tmpDir)) os.remove(tmpDir)
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
        val path = dir / fileName
        if (os.exists(path)) // isFile is false for domain sockets
          os.remove(path)
        (path, true)
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

  def defaultBspSocketOrPort(
    directories: => scala.build.Directories
  ): Option[() => BspConnectionAddress] = {
    def namedSocket =
      Some(() => BspConnectionAddress.UnixDomainSocket(bspSocketFile(directories)))

    def default = namedSocket
    bloopBspProtocol.filter(_ != "default") match {
      case None          => default
      case Some("tcp")   => None
      case Some("local") => namedSocket
      case Some(other) =>
        sys.error(
          s"Invalid bloop BSP protocol value: '$other' (expected 'tcp', 'local', or 'default')"
        )
    }
  }

  private def parseDuration(name: String, valueOpt: Option[String]): Option[FiniteDuration] =
    valueOpt.map(_.trim).filter(_.nonEmpty).map(Duration(_)).map {
      case d: FiniteDuration => d
      case d                 => sys.error(s"Expected finite $name duration, got $d")
    }

  def bloopBspTimeoutDuration: Option[FiniteDuration] =
    parseDuration("BSP connection timeout", bloopBspTimeout)
  def bloopBspCheckPeriodDuration: Option[FiniteDuration] =
    parseDuration("BSP connection check period", bloopBspCheckPeriod)
  def bloopStartupTimeoutDuration: Option[FiniteDuration] =
    parseDuration("connection server startup timeout", bloopStartupTimeout)

  def retainedBloopVersion: BloopRifleConfig.BloopVersionConstraint =
    bloopVersion
      .map(_.trim)
      .filter(_.nonEmpty)
      .fold[BloopRifleConfig.BloopVersionConstraint](BloopRifleConfig.AtLeast(
        BloopVersion(Constants.bloopVersion)
      ))(v => BloopRifleConfig.Strict(BloopVersion(v)))

  def bloopDefaultJvmOptions(logger: Logger): Option[List[String]] = {
    val filePathOpt = bloopGlobalOptionsFile.filter(_.trim.nonEmpty).map(os.Path(_, Os.pwd))
    for (filePath <- filePathOpt)
      yield
        if (os.exists(filePath) && os.isFile(filePath))
          try {
            val content   = os.read.bytes(filePath)
            val bloopJson = readFromArray(content)(BloopJson.codec)
            bloopJson.javaOptions
          }
          catch {
            case e: Throwable =>
              logger.message(s"Error parsing global bloop config in '$filePath':")
              Util.printException(e)
              List.empty
          }
        else {
          logger.message(s"Bloop global options file '$filePath' not found.")
          List.empty
        }
  }

  def bloopRifleConfig(
    logger: Logger,
    cache: FileCache[Task],
    verbosity: Int,
    javaPath: String,
    directories: => scala.build.Directories,
    javaV: Option[Int] = None
  ): BloopRifleConfig = {

    val portOpt = bloopPort.filter(_ != 0) match {
      case Some(n) if n < 0 =>
        Some(scala.build.blooprifle.internal.Util.randomPort())
      case other => other
    }
    val address =
      (
        bloopHost.filter(_.nonEmpty),
        portOpt,
        bloopDaemonDir.filter(_.nonEmpty)
      ) match {
        case (_, _, Some(path)) =>
          BloopRifleConfig.Address.DomainSocket(Paths.get(path))
        case (None, None, None) =>
          val isBloopMainLine = Ver(retainedBloopVersion.version.raw) < Ver("1.4.12")
          if (isBloopMainLine)
            BloopRifleConfig.Address.Tcp(
              host = BloopRifleConfig.defaultHost,
              port = BloopRifleConfig.defaultPort
            )
          else
            BloopRifleConfig.Address.DomainSocket(directories.bloopDaemonDir.toNIO)
        case (hostOpt, portOpt0, _) =>
          BloopRifleConfig.Address.Tcp(
            host = hostOpt.getOrElse(BloopRifleConfig.defaultHost),
            port = portOpt0.getOrElse(BloopRifleConfig.defaultPort)
          )
      }

    val workingDir = bloopWorkingDir
      .filter(_.trim.nonEmpty)
      .map(os.Path(_, Os.pwd))
      .getOrElse(directories.bloopWorkingDir)
    val baseConfig = BloopRifleConfig.default(
      address,
      v => Bloop.bloopClassPath(logger, cache, v),
      workingDir.toIO
    )

    baseConfig.copy(
      javaPath = javaPath,
      bspSocketOrPort = defaultBspSocketOrPort(directories),
      bspStdout = if (verbosity >= 3) Some(System.err) else None,
      bspStderr = Some(System.err),
      period = bloopBspCheckPeriodDuration.getOrElse(baseConfig.period),
      timeout = bloopBspTimeoutDuration.getOrElse(baseConfig.timeout),
      initTimeout = bloopStartupTimeoutDuration.getOrElse(baseConfig.initTimeout),
      javaOpts =
        (if (bloopDefaultJavaOpts) baseConfig.javaOpts
         else Nil) ++ bloopJavaOpt ++ bloopDefaultJvmOptions(logger).getOrElse(Nil),
      minimumBloopJvm = javaV.getOrElse(8),
      retainedBloopVersion = retainedBloopVersion
    )
  }
}

object SharedCompilationServerOptions {
  implicit lazy val parser: Parser[SharedCompilationServerOptions]            = Parser.derive
  implicit lazy val help: Help[SharedCompilationServerOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[SharedCompilationServerOptions] = JsonCodecMaker.make
}
