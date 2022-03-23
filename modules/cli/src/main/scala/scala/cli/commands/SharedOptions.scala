package scala.cli.commands

import caseapp._
import caseapp.core.help.Help
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._
import coursier.util.Artifact
import dependency.AnyDependency
import dependency.parser.DependencyParser

import java.io.{ByteArrayOutputStream, File, InputStream}

import scala.build.blooprifle.BloopRifleConfig
import scala.build.compiler.{BloopCompilerMaker, ScalaCompilerMaker, SimpleScalaCompilerMaker}
import scala.build.internal.CsLoggerUtil._
import scala.build.internal.{Constants, OsLibc}
import scala.build.options.{Platform, ScalacOpt, ShadowingSeq}
import scala.build.{options => bo, _}
import scala.concurrent.duration._
import scala.util.Properties
import scala.util.control.NonFatal

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
  @Recurse
    workspace: SharedWorkspaceOptions = SharedWorkspaceOptions(),

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
  helpGroups: HelpGroupOptions = HelpGroupOptions(),

  @Hidden
    strictBloopJsonCheck: Option[Boolean] = None
) {
  // format: on

  def logger = logging.logger

  def buildOptions(
    enableJmh: Boolean,
    jmhVersion: Option[String],
    ignoreErrors: Boolean = false
  ): bo.BuildOptions = {
    val platformOpt =
      if (js.js) Some(Platform.JS)
      else if (native.native) Some(Platform.Native)
      else None
    bo.BuildOptions(
      scalaOptions = bo.ScalaOptions(
        scalaVersion = scalaVersion.map(_.trim).filter(_.nonEmpty),
        scalaBinaryVersion = scalaBinaryVersion.map(_.trim).filter(_.nonEmpty),
        addScalaLibrary = scalaLibrary.orElse(java.map(!_)),
        generateSemanticDbs = semanticDb,
        scalacOptions = ShadowingSeq.from(
          scalac.scalacOption
            .filter(_.nonEmpty)
            .map(ScalacOpt(_))
            .map(Positioned.commandLine(_))
        ),
        compilerPlugins =
          SharedOptions.parseDependencies(
            dependencies.compilerPlugin.map(Positioned.none(_)),
            ignoreErrors
          ),
        platform = platformOpt.map(o => Positioned(List(Position.CommandLine()), o))
      ),
      scriptOptions = bo.ScriptOptions(
        codeWrapper = None
      ),
      scalaJsOptions = js.scalaJsOptions,
      scalaNativeOptions = native.buildOptions,
      javaOptions = jvm.javaOptions,
      internalDependencies = bo.InternalDependenciesOptions(
        addStubsDependencyOpt = addStubs,
        addRunnerDependencyOpt = runner
      ),
      jmhOptions = bo.JmhOptions(
        addJmhDependencies =
          if (enableJmh) jmhVersion.orElse(Some(Constants.jmhVersion))
          else None,
        runJmh = if (enableJmh) Some(true) else None
      ),
      classPathOptions = bo.ClassPathOptions(
        extraClassPath = extraJars
          .flatMap(_.split(File.pathSeparator).toSeq)
          .filter(_.nonEmpty)
          .map(os.Path(_, os.pwd)),
        extraCompileOnlyJars = extraCompileOnlyJars
          .flatMap(_.split(File.pathSeparator).toSeq)
          .filter(_.nonEmpty)
          .map(os.Path(_, os.pwd)),
        extraRepositories = dependencies.repository.map(_.trim).filter(_.nonEmpty),
        extraDependencies = ShadowingSeq.from(
          SharedOptions.parseDependencies(
            dependencies.dependency.map(Positioned.none(_)),
            ignoreErrors
          )
        )
      ),
      internal = bo.InternalOptions(
        cache = Some(coursierCache),
        localRepository = LocalRepo.localRepo(directories.directories.localRepoDir),
        verbosity = Some(logging.verbosity),
        strictBloopJsonCheck = strictBloopJsonCheck
      ),
      notForBloopOptions = bo.PostBuildOptions(
        scalaJsLinkerOptions = js.linkerOptions
      )
    )
  }

  private def downloadJvm(jvmId: String, options: bo.BuildOptions): String = {
    implicit val ec = options.finalCache.ec
    val javaHomeManager = options.javaHomeManager
      .withMessage(s"Downloading JVM $jvmId")
    val logger = javaHomeManager.cache
      .flatMap(_.archiveCache.cache.loggerOpt)
      .getOrElse(_root_.coursier.cache.CacheLogger.nop)
    val command = {
      val path = logger.use {
        try javaHomeManager.get(jvmId).unsafeRun()
        catch {
          case NonFatal(e) => throw new Exception(e)
        }
      }
      os.Path(path)
    }
    val ext     = if (Properties.isWin) ".exe" else ""
    val javaCmd = (command / "bin" / s"java$ext").toString
    javaCmd
  }

  def bloopRifleConfig(): BloopRifleConfig = {

    val options = buildOptions(false, None)
    lazy val defaultJvmCmd =
      downloadJvm(OsLibc.baseDefaultJvm(OsLibc.jvmIndexOs, "17"), options)
    val javaCmd = compilationServer.bloopJvm.map(downloadJvm(_, options)).orElse {
      for (javaHome <- options.javaHomeLocationOpt()) yield {
        val (javaHomeVersion, javaHomeCmd) = OsLibc.javaHomeVersion(javaHome.value)
        if (javaHomeVersion >= 17) javaHomeCmd
        else defaultJvmCmd
      }
    }.getOrElse(defaultJvmCmd)

    compilationServer.bloopRifleConfig(
      logging.logger,
      coursierCache,
      logging.verbosity,
      javaCmd,
      directories.directories,
      Some(17)
    )
  }

  def compilerMaker(threads: BuildThreads): ScalaCompilerMaker =
    if (compilationServer.server.getOrElse(true))
      new BloopCompilerMaker(
        bloopRifleConfig(),
        threads.bloop,
        strictBloopJsonCheckOrDefault
      )
    else
      SimpleScalaCompilerMaker("java", Nil)

  lazy val coursierCache = coursier.coursierCache(logging.logger.coursierLogger(""))

  def inputsOrExit(
    args: RemainingArgs,
    defaultInputs: () => Option[Inputs] = () => Inputs.default()
  ): Inputs = inputsOrExit(args.all, defaultInputs)

  def inputsOrExit(
    args: Seq[String]
  ): Inputs =
    inputsOrExit(args, () => Inputs.default())
  def inputsOrExit(
    args: Seq[String],
    defaultInputs: () => Option[Inputs]
  ): Inputs = {
    val download: String => Either[String, Array[Byte]] = { url =>
      val artifact = Artifact(url).withChanging(true)
      val res = coursierCache.logger.use {
        try coursierCache.withTtl(0.seconds).file(artifact).run.unsafeRun()(coursierCache.ec)
        catch {
          case NonFatal(e) => throw new Exception(e)
        }
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
      acceptFds = !Properties.isWin,
      forcedWorkspace = workspace.forcedWorkspaceOpt
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

  def strictBloopJsonCheckOrDefault =
    strictBloopJsonCheck.getOrElse(bo.InternalOptions.defaultStrictBloopJsonCheck)
}

object SharedOptions {
  implicit lazy val parser: Parser[SharedOptions]            = Parser.derive
  implicit lazy val help: Help[SharedOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[SharedOptions] = JsonCodecMaker.make

  def parseDependencies(
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
