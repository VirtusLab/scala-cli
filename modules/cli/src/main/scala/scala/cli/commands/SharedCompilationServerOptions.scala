package scala.cli.commands

import caseapp._
import com.google.gson.Gson
import coursier.core.Version

import java.io.{BufferedReader, File, FileReader}
import java.nio.file.{AtomicMoveNotSupportedException, FileAlreadyExistsException, Files}
import java.util.{Locale, Random}

import scala.build.blooprifle.internal.Constants
import scala.build.blooprifle.{BloopRifleConfig, BspConnectionAddress}
import scala.build.{Bloop, Logger, Os}
import scala.cli.internal.Pid
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Properties

// format: off
final case class SharedCompilationServerOptions(
  @Group("Compilation server")
  @HelpMessage("Protocol to use to open a BSP connection with Bloop (ignored on Windows for now)")
  @ValueDescription("tcp|local|default")
  @Hidden
    bloopBspProtocol: Option[String] = None,
  @Group("Compilation server")
  @HelpMessage("Socket file to use to open a BSP connection with Bloop (on Windows, pipe name like \"\\\\.\\pipe\\…\")")
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
    bloopStartupTimeout: Option[String] = None,

  @Group("Compilation server")
    bloopDefaultJavaOpts: Boolean = true,
  @Group("Compilation server")
    bloopJavaOpt: List[String] = Nil,
  @Group("Compilation server")
  @HelpMessage("Bloop global options file")
    bloopGlobalOptionsFile: String = (os.home / ".bloop" / "bloop.json").toString,
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

  def defaultBspSocketOrPort(
    directories: => scala.build.Directories
  ): Option[() => BspConnectionAddress] = {
    def namedSocket =
      if (Properties.isWin)
        Some(() => BspConnectionAddress.WindowsNamedPipe(bspPipeName()))
      else
        Some(() => BspConnectionAddress.UnixDomainSocket(bspSocketFile(directories)))
    def default =
      if (isGraalvmNativeImage && arch != "x86_64")
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

  def minimumBloopVersion = Constants.bloopVersion
  def acceptBloopVersion  = Some((v: String) => Version(v) < Version(minimumBloopVersion))

  def bloopDefaultJvmOptions(logger: Logger): List[String] = {
    val file = new File(bloopGlobalOptionsFile)
    if (file.exists() && file.isFile())
      try {
        val reader = new BufferedReader(new FileReader(file))
        val gson   = new Gson()
        val json   = gson.fromJson(reader, classOf[BloopJson])
        json.javaOptions.toList
      }
      catch {
        case e: Throwable =>
          e.printStackTrace()
          List.empty
      }
    else {
      logger.debug(s"Bloop global options file '${file.toPath().toAbsolutePath()}' not found.")
      List.empty
    }
  }

  def bloopRifleConfig(
    logger: Logger,
    verbosity: Int,
    javaPath: String,
    directories: => scala.build.Directories
  ): BloopRifleConfig = {
    val baseConfig =
      BloopRifleConfig.default(() => Bloop.bloopClassPath(logger))
    val portOpt = bloopPort.filter(_ != 0) match {
      case Some(n) if n < 0 =>
        Some(scala.build.blooprifle.internal.Util.randomPort())
      case other => other
    }
    baseConfig.copy(
      host = bloopHost.filter(_.nonEmpty).getOrElse(baseConfig.host),
      port = portOpt.getOrElse(baseConfig.port),
      javaPath = javaPath,
      bspSocketOrPort = defaultBspSocketOrPort(directories),
      bspStdout = if (verbosity >= 3) Some(System.err) else None,
      bspStderr = if (verbosity >= 3) Some(System.err) else None,
      period = bloopBspCheckPeriodDuration.getOrElse(baseConfig.period),
      timeout = bloopBspTimeoutDuration.getOrElse(baseConfig.timeout),
      initTimeout = bloopStartupTimeoutDuration.getOrElse(baseConfig.initTimeout),
      javaOpts =
        (if (bloopDefaultJavaOpts) baseConfig.javaOpts
         else Nil) ++ bloopJavaOpt ++ bloopDefaultJvmOptions(logger),
      acceptBloopVersion = acceptBloopVersion
    )
  }
}

object SharedCompilationServerOptions {
  implicit val parser = Parser[SharedCompilationServerOptions]
  implicit val help   = Help[SharedCompilationServerOptions]

  private def isGraalvmNativeImage: Boolean =
    sys.props.contains("org.graalvm.nativeimage.imagecode")
  private def arch = sys.props("os.arch").toLowerCase(Locale.ROOT) match {
    case "amd64" => "x86_64"
    case other   => other
  }
}
