package scala.cli.commands

import caseapp._
import caseapp.core.help.Help
import coursier.util.Artifact
import dependency.AnyDependency
import dependency.parser.DependencyParser
import upickle.default.{ReadWriter, macroRW}

import java.io.{ByteArrayOutputStream, File, InputStream}

import scala.build.blooprifle.BloopRifleConfig
import scala.build.internal.{Constants, OsLibc}
import scala.build.options._
import scala.build.{Inputs, LocalRepo, Logger, Os, Position, Positioned}
import scala.concurrent.duration._
import scala.util.Properties
// format: off
final case class SharedOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    js: ScalaJsOptions = ScalaJsOptions(),
  @Recurse
    native: ScalaNativeOptions = ScalaNativeOptions(),
  @Recurse
    compilationServer: SharedCompilationServerOptions = SharedCompilationServerOptions(),
  @Recurse
    directories: SharedDirectoriesOptions = SharedDirectoriesOptions(),
  @Recurse
    dependencies: SharedDependencyOptions = SharedDependencyOptions(),
  @Recurse
    scalac: ScalacOptions = ScalacOptions(),
  @Recurse
    jvm: SharedJvmOptions = SharedJvmOptions(),
  @Recurse
    coursier: CoursierOptions = CoursierOptions(),

  @Group("Scala")
  @HelpMessage("Set the Scala version")
  @ValueDescription("version")
  @Name("scala")
  @Name("S")
    scalaVersion: Option[String] = None,
  @Group("Scala")
  @HelpMessage("Set the Scala binary version")
  @ValueDescription("version")
  @Hidden
  @Name("scalaBinary")
  @Name("scalaBin")
  @Name("B")
    scalaBinaryVersion: Option[String] = None,

  @Group("Java")
  @HelpMessage("Add extra JARs in the class path")
  @ValueDescription("paths")
  @Name("jar")
  @Name("jars")
  @Name("extraJar")
    extraJars: List[String] = Nil,

  @Group("Java")
  @HelpMessage("Add extra JARs in the class path, during compilation only")
  @ValueDescription("paths")
  @Name("compileOnlyJar")
  @Name("compileOnlyJars")
  @Name("extraCompileOnlyJar")
    extraCompileOnlyJars: List[String] = Nil,

  @Group("Java")
  @HelpMessage("Add extra source JARs")
  @ValueDescription("paths")
  @Name("sourceJar")
  @Name("sourceJars")
  @Name("extraSourceJar")
    extraSourceJars: List[String] = Nil,

  @Group("Java")
  @HelpMessage("Add a resource directory")
  @ValueDescription("paths")
  @Name("resourceDir")
    resourceDirs: List[String] = Nil,

  @Group("Scala")
  @Hidden
    scalaLibrary: Option[Boolean] = None,
  @Group("Java")
  @Hidden
    java: Option[Boolean] = None,
  @Hidden
    runner: Option[Boolean] = None,

  @Hidden
  @HelpMessage("Generate SemanticDBs")
    semanticDb: Option[Boolean] = None,
  @Hidden
    addStubs: Option[Boolean] = None,

  @Hidden
    defaultForbiddenDirectories: Boolean = true,
  @Hidden
    forbid: List[String] = Nil,
  @Recurse
  helpGroups: HelpGroupOptions = HelpGroupOptions()
) {
  // format: on

  def logger = logging.logger

  private def parseDependencies(
    deps: List[Positioned[String]],
    ignoreErrors: Boolean
  ): Seq[Positioned[AnyDependency]] =
    deps.map(_.map(_.trim)).filter(_.value.nonEmpty)
      .flatMap { posDepStr =>
        val depStr = posDepStr.value
        DependencyParser.parse(depStr) match {
          case Left(err) =>
            if (ignoreErrors) Nil
            else sys.error(s"Error parsing dependency '$depStr': $err")
          case Right(dep) => Seq(posDepStr.map(_ => dep))
        }
      }

  def buildOptions(
    enableJmh: Boolean,
    jmhVersion: Option[String],
    ignoreErrors: Boolean = false
  ): BuildOptions = {
    val platformOpt =
      if (js.js) Some(Platform.JS)
      else if (native.native) Some(Platform.Native)
      else None
    BuildOptions(
      scalaOptions = ScalaOptions(
        scalaVersion = scalaVersion.map(_.trim).filter(_.nonEmpty),
        scalaBinaryVersion = scalaBinaryVersion.map(_.trim).filter(_.nonEmpty),
        addScalaLibrary = scalaLibrary.orElse(java.map(!_)),
        generateSemanticDbs = semanticDb,
        scalacOptions = scalac.scalacOption.filter(_.nonEmpty),
        compilerPlugins =
          parseDependencies(dependencies.compilerPlugin.map(Positioned.none(_)), ignoreErrors),
        platform = platformOpt.map(o => Positioned(List(Position.CommandLine()), o))
      ),
      scriptOptions = ScriptOptions(
        codeWrapper = None
      ),
      scalaJsOptions = js.buildOptions,
      scalaNativeOptions = native.buildOptions,
      javaOptions = jvm.javaOptions,
      internalDependencies = InternalDependenciesOptions(
        addStubsDependencyOpt = addStubs,
        addRunnerDependencyOpt = runner
      ),
      jmhOptions = JmhOptions(
        addJmhDependencies =
          if (enableJmh) jmhVersion.orElse(Some(Constants.jmhVersion))
          else None,
        runJmh = if (enableJmh) Some(true) else None
      ),
      classPathOptions = ClassPathOptions(
        extraClassPath = extraJars
          .flatMap(_.split(File.pathSeparator).toSeq)
          .filter(_.nonEmpty)
          .map(os.Path(_, os.pwd)),
        extraCompileOnlyJars = extraCompileOnlyJars
          .flatMap(_.split(File.pathSeparator).toSeq)
          .filter(_.nonEmpty)
          .map(os.Path(_, os.pwd)),
        extraRepositories = dependencies.repository.map(_.trim).filter(_.nonEmpty),
        extraDependencies =
          parseDependencies(dependencies.dependency.map(Positioned.none(_)), ignoreErrors)
      ),
      internal = InternalOptions(
        cache = Some(coursierCache),
        localRepository = LocalRepo.localRepo(directories.directories.localRepoDir),
        verbosity = Some(logging.verbosity)
      )
    )
  }

  def bloopRifleConfig(): BloopRifleConfig = {

    val options     = buildOptions(false, None)
    implicit val ec = options.finalCache.ec
    val jvmId = compilationServer.bloopJvm.getOrElse {
      OsLibc.baseDefaultJvm(OsLibc.jvmIndexOs, "17")
    }
    val logger = options.javaHomeManager.cache
      .flatMap(_.archiveCache.cache.loggerOpt)
      .getOrElse(_root_.coursier.cache.CacheLogger.nop)
    val command = os.Path(logger.use(options.javaHomeManager.get(jvmId).unsafeRun()))
    val ext     = if (Properties.isWin) ".exe" else ""
    compilationServer.bloopRifleConfig(
      logging.logger,
      logging.verbosity,
      (command / "bin" / s"java$ext").toString,
      directories.directories,
      Some(17)
    )
  }

  lazy val coursierCache = coursier.coursierCache(logging.logger.coursierLogger)

  def inputsOrExit(
    args: RemainingArgs,
    defaultInputs: () => Option[Inputs] = () => Inputs.default()
  ): Inputs = inputsOrExit(args.remaining, defaultInputs)

  def inputsOrExit(
    args: Seq[String],
    defaultInputs: () => Option[Inputs]
  ): Inputs = {
    val download: String => Either[String, Array[Byte]] = { url =>
      val artifact = Artifact(url).withChanging(true)
      val res = coursierCache.logger.use {
        coursierCache.withTtl(0.seconds).file(artifact).run.unsafeRun()(coursierCache.ec)
      }
      res
        .left.map(_.describe)
        .map(f => os.read.bytes(os.Path(f, Os.pwd)))
    }
    val resourceInputs = resourceDirs
      .map(os.Path(_, Os.pwd))
      .map { path =>
        if (!os.exists(path))
          logger.message(s"WARNING: provided resource directory path doesn't exist: $path")
        path
      }
      .map(Inputs.ResourceDirectory(_))
    val inputs = Inputs(
      args,
      Os.pwd,
      directories.directories,
      defaultInputs = defaultInputs,
      download = download,
      stdinOpt = SharedOptions.readStdin(logger = logger),
      acceptFds = !Properties.isWin
    ) match {
      case Left(message) =>
        System.err.println(message)
        sys.exit(1)
      case Right(i) => i
    }
    val forbiddenDirs =
      (if (defaultForbiddenDirectories) SharedOptions.defaultForbiddenDirectories else Nil) ++
        forbid.filter(_.trim.nonEmpty).map(os.Path(_, Os.pwd))

    inputs
      .add(resourceInputs)
      .checkAttributes(directories.directories)
      .avoid(forbiddenDirs, directories.directories)
  }
}

object SharedOptions {
  implicit lazy val parser: Parser[SharedOptions]        = Parser.derive
  implicit lazy val help: Help[SharedOptions]            = Help.derive
  implicit lazy val jsonCodec: ReadWriter[SharedOptions] = macroRW

  def readStdin(in: InputStream = System.in, logger: Logger): Option[Array[Byte]] =
    if (in == null) {
      logger.debug("No stdin available")
      None
    }
    else {
      logger.debug("Reading stdin")
      val baos = new ByteArrayOutputStream
      val buf  = Array.ofDim[Byte](16 * 1024)
      var read = -1
      while ({
        read = in.read(buf)
        read >= 0
      })
        if (read > 0)
          baos.write(buf, 0, read)
      val result = baos.toByteArray
      logger.debug(s"Done reading stdin (${result.length} B)")
      Some(result)
    }

  private def defaultForbiddenDirectories: Seq[os.Path] =
    if (Properties.isWin)
      Seq(os.Path("""C:\Windows\System32"""))
    else
      Nil

}
