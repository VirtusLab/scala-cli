package scala.cli.commands

import caseapp._
import coursier.core.{Version => Ver}
import upickle.default.{ReadWriter, macroRW}

import java.io.File
import java.nio.charset.Charset
import java.nio.file.{AtomicMoveNotSupportedException, FileAlreadyExistsException, Files, Paths}
import java.util.{Locale, Random}

import scala.build.blooprifle.internal.Constants
import scala.build.blooprifle.{BloopRifleConfig, BloopVersion, BspConnectionAddress}
import scala.build.{Bloop, Logger, Os}
import scala.cli.internal.Pid
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.io.Codec
import scala.util.Properties

// format: off
final case class SharedCompilationServerOptions(
  @Group("Compilation server")
  @HelpMessage("Protocol to use to open a BSP connection with Bloop")
  @ValueDescription("tcp|local|default")
  @Hidden
    bloopBspProtocol: Option[String] = None,
  @Group("Compilation server")
  @HelpMessage("Socket file to use to open a BSP connection with Bloop (on Windows, a pipe name like \"`\\\\.\\pipe\\…`\")")
  @ValueDescription("path")
  @Hidden
    bloopBspSocket: Option[String] = None,

  @Group("Compilation server")
  @HelpMessage("Host the compilation server should bind to")
  @ValueDescription("host")
  @Hidden
    bloopHost: Option[String] = None,
  @Group("Compilation server")
  @HelpMessage("Port the compilation server should bind to (pass `-1` to pick a random port)")
  @ValueDescription("port|-1")
  @Hidden
    bloopPort: Option[Int] = None,
  @Group("Compilation server")
  @HelpMessage("Daemon directory of the Bloop daemon (directory with lock, pid, and socket files)")
  @ValueDescription("path")
  @Hidden
    bloopDaemonDir: Option[String] = None,
  @Group("Compilation server")
  @HelpMessage("Pipe name of the Bloop daemon (Windows x86_64 only)")
  @ValueDescription("name")
  @Hidden
    bloopPipeName: Option[String] = None,

  @Group("Compilation server")
  @HelpMessage("If Bloop isn't already running, the version we should start")
  @ValueDescription("version")
  @Hidden
    bloopVersion: Option[String] = None,

  @Hidden
  @Group("Compilation server")
  @HelpMessage("Maximum duration to wait for the BSP connection to be opened")
  @ValueDescription("duration")
    bloopBspTimeout: Option[String] = None,
  @Hidden
  @Group("Compilation server")
  @HelpMessage("Duration between checks of the BSP connection state")
  @ValueDescription("duration")
    bloopBspCheckPeriod: Option[String] = None,
  @Hidden
  @Group("Compilation server")
  @HelpMessage("Maximum duration to wait for the compilation server to start up")
  @ValueDescription("duration")
    bloopStartupTimeout: Option[String] = None,

  @Group("Compilation server")
  @HelpMessage("Include default JVM options for Bloop")
  @Hidden
    bloopDefaultJavaOpts: Boolean = true,
  @Group("Compilation server")
  @Hidden
    bloopJavaOpt: List[String] = Nil,
  @Group("Compilation server")
  @HelpMessage("Bloop global options file")
  @Hidden
    bloopGlobalOptionsFile: String = (os.home / ".bloop" / "bloop.json").toString,

  @Group("Compilation server")
  @HelpMessage("JVM to use to start Bloop (e.g. 'system|11', 'temurin:17', …)")
  @Hidden
    bloopJvm: Option[String] = None
) {
  // format: on

  import SharedCompilationServerOptions.{arch, isGraalvmNativeImage}

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

  private def bspPipeName(): String =
    bloopBspSocket.filter(_.nonEmpty).getOrElse {
      val bt = "\\"
      s"$bt$bt.${bt}pipe$bt" + pidOrRandom
        .map("proc-" + _)
        .left.map("conn-" + _)
        .merge
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
      if (Properties.isWin)
        Some(() => BspConnectionAddress.WindowsNamedPipe(bspPipeName()))
      else
        Some(() => BspConnectionAddress.UnixDomainSocket(bspSocketFile(directories)))

    // FreeBSD and others throw a java.lang.UnsatisfiedLinkError when trying the
    // UnixDomainSocket, because of the ipcsocket JNI stuff, so stick with TCP for them.
    def isStandardOs = Properties.isLinux || Properties.isWin || Properties.isMac
    def default =
      if ((isGraalvmNativeImage && arch != "x86_64") || !isStandardOs)
        None // tcp
      else
        namedSocket
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

  lazy val retainedBloopVersion: BloopRifleConfig.BloopVersionConstraint =
    bloopVersion
      .map(_.trim)
      .filter(_.nonEmpty)
      .fold[BloopRifleConfig.BloopVersionConstraint](BloopRifleConfig.AtLeast(
        BloopVersion(Constants.bloopVersion)
      ))(v => BloopRifleConfig.Strict(BloopVersion(v)))

  def bloopDefaultJvmOptions(logger: Logger): List[String] = {
    val filePath = os.Path(bloopGlobalOptionsFile, Os.pwd)
    if (os.exists(filePath) && os.isFile(filePath))
      try {
        val json = ujson.read(
          os.read(filePath: os.ReadablePath, charSet = Codec(Charset.defaultCharset()))
        )
        val bloopJson = upickle.default.read(json)(BloopJson.jsonCodec)
        bloopJson.javaOptions
      }
      catch {
        case e: Throwable =>
          System.err.println(s"Error parsing global bloop config in '$filePath':")
          e.printStackTrace()
          List.empty
      }
    else {
      logger.debug(s"Bloop global options file '$filePath' not found.")
      List.empty
    }
  }

  def bloopRifleConfig(
    logger: Logger,
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
        bloopDaemonDir.filter(_.nonEmpty),
        bloopPipeName.filter(_.nonEmpty)
      ) match {
        case (_, _, Some(path), pipeNameOpt) =>
          BloopRifleConfig.Address.DomainSocket(
            Paths.get(path),
            pipeNameOpt.getOrElse(SharedCompilationServerOptions.defaultBloopPipeName)
          )
        case (None, None, None, pipeNameOpt) =>
          val isBloopMainLine = Ver(retainedBloopVersion.version.raw) < Ver("1.4.12")
          if (isBloopMainLine || Properties.isWin)
            BloopRifleConfig.Address.Tcp(
              host = BloopRifleConfig.defaultHost,
              port = BloopRifleConfig.defaultPort
            )
          else
            BloopRifleConfig.Address.DomainSocket(
              directories.bloopDaemonDir.toNIO,
              pipeNameOpt.getOrElse(SharedCompilationServerOptions.defaultBloopPipeName)
            )
        case (hostOpt, portOpt0, _, _) =>
          BloopRifleConfig.Address.Tcp(
            host = bloopHost.filter(_.nonEmpty).getOrElse(BloopRifleConfig.defaultHost),
            port = portOpt0.getOrElse(BloopRifleConfig.defaultPort)
          )
      }

    val baseConfig = BloopRifleConfig.default(
      address,
      v => Bloop.bloopClassPath(logger, v)
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
         else Nil) ++ bloopJavaOpt ++ bloopDefaultJvmOptions(logger),
      minimumBloopJvm = javaV.getOrElse(8),
      retainedBloopVersion = retainedBloopVersion
    )
  }
}

object SharedCompilationServerOptions {
  lazy val parser: Parser[SharedCompilationServerOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[SharedCompilationServerOptions, parser.D] = parser
  implicit lazy val help: Help[SharedCompilationServerOptions]                      = Help.derive
  implicit lazy val jsonCodec: ReadWriter[SharedCompilationServerOptions]           = macroRW

  private def isGraalvmNativeImage: Boolean =
    sys.props.contains("org.graalvm.nativeimage.imagecode")
  private def arch = sys.props("os.arch").toLowerCase(Locale.ROOT) match {
    case "amd64" => "x86_64"
    case other   => other
  }

  private def defaultBloopPipeName =
    "scalacli\\bloop\\pipe"
}
