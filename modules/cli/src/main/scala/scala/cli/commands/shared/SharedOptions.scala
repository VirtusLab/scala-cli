package scala.cli.commands.shared

import caseapp.*
import caseapp.core.help.Help
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import coursier.cache.FileCache
import coursier.util.{Artifact, Task}
import dependency.AnyDependency
import dependency.parser.DependencyParser

import java.io.{File, InputStream}
import java.nio.file.Paths

import scala.build.EitherCps.{either, value}
import scala.build.*
import scala.build.blooprifle.BloopRifleConfig
import scala.build.compiler.{BloopCompilerMaker, ScalaCompilerMaker, SimpleScalaCompilerMaker}
import scala.build.directives.DirectiveDescription
import scala.build.errors.{AmbiguousPlatformError, BuildException}
import scala.build.input.{Element, Inputs, ResourceDirectory, ScalaCliInvokeData}
import scala.build.interactive.Interactive
import scala.build.interactive.Interactive.{InteractiveAsk, InteractiveNop}
import scala.build.internal.CsLoggerUtil.*
import scala.build.internal.{Constants, FetchExternalBinary, OsLibc, Util}
import scala.build.options.ScalaVersionUtil.fileWithTtl0
import scala.build.options.{Platform, ScalacOpt, ShadowingSeq}
import scala.build.preprocessing.directives.Toolkit
import scala.build.options as bo
import scala.cli.ScalaCli
import scala.cli.commands.publish.ConfigUtil.*
import scala.cli.commands.shared.{
  HasLoggingOptions,
  ScalaJsOptions,
  ScalaNativeOptions,
  SharedOptions,
  SuppressWarningOptions
}
import scala.cli.commands.tags
import scala.cli.commands.util.ScalacOptionsUtil.*
import scala.cli.config.Key.BooleanEntry
import scala.cli.config.{ConfigDb, ConfigDbException, Keys}
import scala.concurrent.ExecutionContextExecutorService
import scala.concurrent.duration.*
import scala.util.Properties
import scala.util.control.NonFatal

// format: off
final case class SharedOptions(
  @Recurse
    suppress: SuppressWarningOptions = SuppressWarningOptions(),
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    js: ScalaJsOptions = ScalaJsOptions(),
  @Recurse
    native: ScalaNativeOptions = ScalaNativeOptions(),
  @Recurse
    compilationServer: SharedCompilationServerOptions = SharedCompilationServerOptions(),
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
  @Recurse
    sharedPython: SharedPythonOptions = SharedPythonOptions(),

  @Group(HelpGroup.Scala.toString)
  @HelpMessage(s"Set the Scala version (${Constants.defaultScalaVersion} by default)")
  @ValueDescription("version")
  @Name("scala")
  @Name("S")
  @Tag(tags.must)
    scalaVersion: Option[String] = None,
  @Group(HelpGroup.Scala.toString)
  @HelpMessage("Set the Scala binary version")
  @ValueDescription("version")
  @Hidden
  @Name("scalaBinary")
  @Name("scalaBin")
  @Name("B")
  @Tag(tags.must)
    scalaBinaryVersion: Option[String] = None,

  @Recurse
    scalacExtra: ScalacExtraOptions = ScalacExtraOptions(),

  @Recurse
    snippet: SnippetOptions = SnippetOptions(),

  @Recurse
    markdown: MarkdownOptions = MarkdownOptions(),

  @Group(HelpGroup.Java.toString)
  @HelpMessage("Add extra JARs and compiled classes to the class path")
  @ValueDescription("paths")
  @Name("jar")
  @Name("jars")
  @Name("extraJar")
  @Name("class")
  @Name("extraClass")
  @Name("classes")
  @Name("extraClasses")
  @Name("-classpath")
  @Name("-cp")
  @Name("classpath")
  @Name("classPath")
  @Name("extraClassPath")
  @Tag(tags.must)
    extraJars: List[String] = Nil,

  @Group(HelpGroup.Java.toString)
  @HelpMessage("Add extra JARs in the compilaion class path. Mainly using to run code in managed environments like Spark not to include certain depenencies on runtime ClassPath.")
  @ValueDescription("paths")
  @Name("compileOnlyJar")
  @Name("compileOnlyJars")
  @Name("extraCompileOnlyJar")
  @Tag(tags.should)
    extraCompileOnlyJars: List[String] = Nil,

  @Group(HelpGroup.Java.toString)
  @HelpMessage("Add extra source JARs")
  @ValueDescription("paths")
  @Name("sourceJar")
  @Name("sourceJars")
  @Name("extraSourceJar")
  @Tag(tags.should)
    extraSourceJars: List[String] = Nil,

  @Group(HelpGroup.Java.toString)
  @HelpMessage("Add a resource directory")
  @ValueDescription("paths")
  @Name("resourceDir")
  @Tag(tags.must)
    resourceDirs: List[String] = Nil,

  @Group(HelpGroup.Scala.toString)
  @HelpMessage("Specify platform")
  @ValueDescription("scala-js|scala-native|jvm")
  @Tag(tags.should)
  @Tag(tags.inShortHelp)
    platform: Option[String] = None,

  @Group(HelpGroup.Scala.toString)
  @Tag(tags.implementation)
  @Hidden
    scalaLibrary: Option[Boolean] = None,
  @Group(HelpGroup.Scala.toString)
  @HelpMessage("Allows to include the Scala compiler artifacts on the classpath.")
  @Tag(tags.must)
  @Name("withScalaCompiler")
  @Name("-with-compiler")
    withCompiler: Option[Boolean] = None,
  @Group(HelpGroup.Java.toString)
  @HelpMessage("Do not add dependency to Scala Standard library. This is useful, when Scala CLI works with pure Java projects.")
  @Tag(tags.implementation)
  @Hidden
    java: Option[Boolean] = None,
  @Group(HelpGroup.Scala.toString)
  @HelpMessage("Should include Scala CLI runner on the runtime ClassPath. Runner is added by default for application running on JVM using standard Scala versions. Runner is used to make stack traces more readable in case of application failure.")
  @Tag(tags.implementation)
  @Hidden
    runner: Option[Boolean] = None,

  @Hidden
  @Tag(tags.should)
  @HelpMessage("Generate SemanticDBs")
    semanticDb: Option[Boolean] = None,

  @Recurse
    input: SharedInputOptions = SharedInputOptions(),
  @Recurse
    helpGroups: HelpGroupOptions = HelpGroupOptions(),

  @Hidden
    strictBloopJsonCheck: Option[Boolean] = None,

  @Group(HelpGroup.Scala.toString)
  @Name("output-directory")
  @Name("d")
  @Name("destination")
  @Name("compileOutput")
  @Name("compileOut")
  @HelpMessage("Copy compilation results to output directory using either relative or absolute path")
  @ValueDescription("/example/path")
  @Tag(tags.must)
    compilationOutput: Option[String] = None,
  @Group(HelpGroup.Scala.toString)
  @HelpMessage("Add toolkit to classPath")
  @ValueDescription("version|latest")
  @Name("toolkit")
  @Tag(tags.implementation)
  @Tag(tags.inShortHelp)
    withToolkit: Option[String] = None
) extends HasLoggingOptions {
  // format: on

  def logger: Logger = logging.logger

  private def scalaJsOptions(opts: ScalaJsOptions): options.ScalaJsOptions = {
    import opts._
    options.ScalaJsOptions(
      version = jsVersion,
      mode = jsMode,
      moduleKindStr = jsModuleKind,
      checkIr = jsCheckIr,
      emitSourceMaps = jsEmitSourceMaps,
      sourceMapsDest = jsSourceMapsPath.filter(_.trim.nonEmpty).map(os.Path(_, Os.pwd)),
      dom = jsDom,
      header = jsHeader,
      allowBigIntsForLongs = jsAllowBigIntsForLongs,
      avoidClasses = jsAvoidClasses,
      avoidLetsAndConsts = jsAvoidLetsAndConsts,
      moduleSplitStyleStr = jsModuleSplitStyle,
      smallModuleForPackage = jsSmallModuleForPackage,
      esVersionStr = jsEsVersion
    )
  }

  private def linkerOptions(opts: ScalaJsOptions): options.scalajs.ScalaJsLinkerOptions = {
    import opts._
    options.scalajs.ScalaJsLinkerOptions(
      linkerPath = jsLinkerPath
        .filter(_.trim.nonEmpty)
        .map(os.Path(_, Os.pwd)),
      scalaJsVersion = jsVersion.map(_.trim).filter(_.nonEmpty),
      scalaJsCliVersion = jsCliVersion.map(_.trim).filter(_.nonEmpty),
      javaArgs = jsCliJavaArg,
      useJvm = jsCliOnJvm.map {
        case false => Left(FetchExternalBinary.platformSuffix())
        case true  => Right(())
      }
    )
  }

  private def scalaNativeOptions(opts: ScalaNativeOptions): options.ScalaNativeOptions = {
    import opts._
    options.ScalaNativeOptions(
      nativeVersion,
      nativeMode,
      nativeGc,
      nativeClang,
      nativeClangpp,
      nativeLinking,
      nativeLinkingDefaults,
      nativeCompile,
      nativeCompileDefaults
    )
  }

  def buildOptions(
    enableJmh: Boolean = false,
    jmhVersion: Option[String] = None,
    ignoreErrors: Boolean = false
  ): Either[BuildException, bo.BuildOptions] = either {
    val releaseOpt = scalac.scalacOption.getScalacOption("-release")
    val targetOpt  = scalac.scalacOption.getScalacPrefixOption("-target")
    jvm.jvm -> (releaseOpt.toSeq ++ targetOpt) match {
      case (Some(j), compilerTargets) if compilerTargets.exists(_ != j) =>
        val compilerTargetsString = compilerTargets.distinct.mkString(", ")
        logger.error(
          s"Warning: different target JVM ($j) and scala compiler target JVM ($compilerTargetsString) were passed."
        )
      case _ =>
    }
    val parsedPlatform = platform.map(Platform.normalize).flatMap(Platform.parse)
    val platformOpt = value {
      (parsedPlatform, js.js, native.native) match {
        case (Some(p: Platform.JS.type), _, false)      => Right(Some(p))
        case (Some(p: Platform.Native.type), false, _)  => Right(Some(p))
        case (Some(p: Platform.JVM.type), false, false) => Right(Some(p))
        case (Some(p), _, _) =>
          val jsSeq        = if (js.js) Seq(Platform.JS) else Seq.empty
          val nativeSeq    = if (native.native) Seq(Platform.Native) else Seq.empty
          val platformsSeq = Seq(p) ++ jsSeq ++ nativeSeq
          Left(new AmbiguousPlatformError(platformsSeq.distinct.map(_.toString)))
        case (_, true, true) =>
          Left(new AmbiguousPlatformError(Seq(Platform.JS.toString, Platform.Native.toString)))
        case (_, true, _) => Right(Some(Platform.JS))
        case (_, _, true) => Right(Some(Platform.Native))
        case _            => Right(None)
      }
    }
    bo.BuildOptions(
      suppressWarningOptions =
        bo.SuppressWarningOptions(
          getOptionOrFromConfig(
            suppress.suppressDirectivesInMultipleFilesWarning,
            Keys.suppressDirectivesInMultipleFilesWarning
          ),
          getOptionOrFromConfig(
            suppress.suppressOutdatedDependencyWarning,
            Keys.suppressOutdatedDependenciessWarning
          )
        ),
      scalaOptions = bo.ScalaOptions(
        scalaVersion = scalaVersion
          .map(_.trim)
          .filter(_.nonEmpty)
          .map(bo.MaybeScalaVersion(_)),
        scalaBinaryVersion = scalaBinaryVersion.map(_.trim).filter(_.nonEmpty),
        addScalaLibrary = scalaLibrary.orElse(java.map(!_)),
        addScalaCompiler = withCompiler,
        generateSemanticDbs = semanticDb,
        scalacOptions = scalac
          .scalacOption
          .withScalacExtraOptions(scalacExtra)
          .toScalacOptShadowingSeq
          .filterNonRedirected
          .filterNonDeprecated
          .map(Positioned.commandLine),
        compilerPlugins =
          SharedOptions.parseDependencies(
            dependencies.compilerPlugin.map(Positioned.none),
            ignoreErrors
          ),
        platform = platformOpt.map(o => Positioned(List(Position.CommandLine()), o))
      ),
      scriptOptions = bo.ScriptOptions(
        codeWrapper = None
      ),
      scalaJsOptions = scalaJsOptions(js),
      scalaNativeOptions = scalaNativeOptions(native),
      javaOptions = value(scala.cli.commands.util.JvmUtils.javaOptions(jvm)),
      jmhOptions = bo.JmhOptions(
        addJmhDependencies =
          if (enableJmh) jmhVersion.orElse(Some(Constants.jmhVersion))
          else None,
        runJmh = if (enableJmh) Some(true) else None
      ),
      classPathOptions = bo.ClassPathOptions(
        extraClassPath = extraJarsAndClassPath,
        extraCompileOnlyJars = extraCompileOnlyClassPath,
        extraRepositories = dependencies.repository.map(_.trim).filter(_.nonEmpty),
        extraDependencies = ShadowingSeq.from(
          SharedOptions.parseDependencies(
            dependencies.dependency.map(Positioned.none),
            ignoreErrors
          ) ++ SharedOptions.resolveToolkitDependency(withToolkit)
        )
      ),
      internal = bo.InternalOptions(
        cache = Some(coursierCache),
        localRepository = LocalRepo.localRepo(Directories.directories.localRepoDir),
        verbosity = Some(logging.verbosity),
        strictBloopJsonCheck = strictBloopJsonCheck,
        interactive = Some(() => interactive)
      ),
      notForBloopOptions = bo.PostBuildOptions(
        scalaJsLinkerOptions = linkerOptions(js),
        addRunnerDependencyOpt = runner,
        python = sharedPython.python,
        pythonSetup = sharedPython.pythonSetup,
        scalaPyVersion = sharedPython.scalaPyVersion
      )
    )
  }

  extension (rawClassPath: List[String]) {
    def extractedClassPath: List[os.Path] =
      rawClassPath
        .flatMap(_.split(File.pathSeparator).toSeq)
        .filter(_.nonEmpty)
        .distinct
        .map(os.Path(_, os.pwd))
        .flatMap {
          case cp if os.isDir(cp) =>
            val jarsInTheDirectory =
              os.walk(cp)
                .filter(p => os.isFile(p) && p.last.endsWith(".jar"))
            List(cp) ++ jarsInTheDirectory // .jar paths have to be passed directly, unlike .class
          case cp => List(cp)
        }
  }

  def extraJarsAndClassPath: List[os.Path] =
    (extraJars ++ scalac.scalacOption.getScalacOption("-classpath"))
      .extractedClassPath

  def extraCompileOnlyClassPath: List[os.Path] = extraCompileOnlyJars.extractedClassPath

  def globalInteractiveWasSuggested: Either[BuildException, Option[Boolean]] = either {
    value(configDb).get(Keys.globalInteractiveWasSuggested) match {
      case Right(opt) => opt
      case Left(ex) =>
        logger.debug(ConfigDbException(ex))
        None
    }
  }

  def interactive: Either[BuildException, Interactive] = either {
    (
      logging.verbosityOptions.interactive,
      value(configDb).get(Keys.interactive) match {
        case Right(opt) => opt
        case Left(ex) =>
          logger.debug(ConfigDbException(ex))
          None
      },
      value(globalInteractiveWasSuggested)
    ) match {
      case (Some(true), _, Some(true)) => InteractiveAsk
      case (_, Some(true), _)          => InteractiveAsk
      case (Some(true), _, _) =>
        val answers @ List(yesAnswer, _) = List("Yes", "No")
        InteractiveAsk.chooseOne(
          s"""You have run the current ${ScalaCli.baseRunnerName} command with the --interactive mode turned on.
             |Would you like to leave it on permanently?""".stripMargin,
          answers
        ) match {
          case Some(answer) if answer == yesAnswer =>
            val configDb0 = value(configDb)
            value {
              configDb0
                .set(Keys.interactive, true)
                .set(Keys.globalInteractiveWasSuggested, true)
                .save(Directories.directories.dbPath.toNIO)
                .wrapConfigException
            }
            logger.message(
              s"--interactive is now set permanently. All future ${ScalaCli.baseRunnerName} commands will run with the flag set to true."
            )
            logger.message(
              s"If you want to turn this setting off at any point, just run `${ScalaCli.baseRunnerName} config interactive false`."
            )
          case _ =>
            val configDb0 = value(configDb)
            value {
              configDb0
                .set(Keys.globalInteractiveWasSuggested, true)
                .save(Directories.directories.dbPath.toNIO)
                .wrapConfigException
            }
            logger.message(
              s"If you want to turn this setting permanently on at any point, just run `${ScalaCli.baseRunnerName} config interactive true`."
            )
        }
        InteractiveAsk
      case _ => InteractiveNop
    }
  }

  def getOptionOrFromConfig(cliOption: Option[Boolean], configDbKey: BooleanEntry) =
    cliOption.orElse(
      configDb.map(_.get(configDbKey))
        .map {
          case Right(opt) => opt
          case Left(ex) =>
            logger.debug(ConfigDbException(ex))
            None
        }
        .getOrElse(None)
    )

  def configDb: Either[BuildException, ConfigDb] =
    ConfigDb.open(Directories.directories.dbPath.toNIO)
      .wrapConfigException

  def downloadJvm(jvmId: String, options: bo.BuildOptions): String = {
    implicit val ec: ExecutionContextExecutorService = options.finalCache.ec
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

  def bloopRifleConfig(): Either[BuildException, BloopRifleConfig] = either {
    val options = value(buildOptions(false, None))
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
      Directories.directories,
      Some(17)
    )
  }

  def compilerMaker(
    threads: BuildThreads,
    scaladoc: Boolean = false
  ): Either[BuildException, ScalaCompilerMaker] = either {
    if (scaladoc)
      SimpleScalaCompilerMaker("java", Nil, scaladoc = true)
    else if (compilationServer.server.getOrElse(true))
      new BloopCompilerMaker(
        value(bloopRifleConfig()),
        threads.bloop,
        strictBloopJsonCheckOrDefault
      )
    else
      SimpleScalaCompilerMaker("java", Nil)
  }

  lazy val coursierCache = coursier.coursierCache(logging.logger.coursierLogger(""))

  def inputs(
    args: Seq[String],
    defaultInputs: () => Option[Inputs] = () => Inputs.default()
  )(using ScalaCliInvokeData): Either[BuildException, Inputs] =
    SharedOptions.inputs(
      args,
      defaultInputs,
      resourceDirs,
      Directories.directories,
      logger = logger,
      coursierCache,
      workspace.forcedWorkspaceOpt,
      input.defaultForbiddenDirectories,
      input.forbid,
      scriptSnippetList = allScriptSnippets,
      scalaSnippetList = allScalaSnippets,
      javaSnippetList = allJavaSnippets,
      markdownSnippetList = allMarkdownSnippets,
      enableMarkdown = markdown.enableMarkdown,
      extraClasspathWasPassed = extraJarsAndClassPath.nonEmpty
    )

  def allScriptSnippets: List[String]   = snippet.scriptSnippet ++ snippet.executeScript
  def allScalaSnippets: List[String]    = snippet.scalaSnippet ++ snippet.executeScala
  def allJavaSnippets: List[String]     = snippet.javaSnippet ++ snippet.executeJava
  def allMarkdownSnippets: List[String] = snippet.markdownSnippet ++ snippet.executeMarkdown

  def validateInputArgs(
    args: Seq[String]
  )(using ScalaCliInvokeData): Seq[Either[String, Seq[Element]]] =
    Inputs.validateArgs(
      args,
      Os.pwd,
      SharedOptions.downloadInputs(coursierCache),
      SharedOptions.readStdin(logger = logger),
      !Properties.isWin,
      enableMarkdown = true
    )

  def strictBloopJsonCheckOrDefault: Boolean =
    strictBloopJsonCheck.getOrElse(bo.InternalOptions.defaultStrictBloopJsonCheck)

}

object SharedOptions {
  implicit lazy val parser: Parser[SharedOptions]            = Parser.derive
  implicit lazy val help: Help[SharedOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[SharedOptions] = JsonCodecMaker.make

  private def downloadInputs(cache: FileCache[Task]): String => Either[String, Array[Byte]] = {
    url =>
      val artifact = Artifact(url).withChanging(true)
      cache.fileWithTtl0(artifact)
        .left
        .map(_.describe)
        .map(f => os.read.bytes(os.Path(f, Os.pwd)))
  }

  /** [[Inputs]] builder, handy when you don't have a [[SharedOptions]] instance at hand */
  def inputs(
    args: Seq[String],
    defaultInputs: () => Option[Inputs],
    resourceDirs: Seq[String],
    directories: scala.build.Directories,
    logger: scala.build.Logger,
    cache: FileCache[Task],
    forcedWorkspaceOpt: Option[os.Path],
    defaultForbiddenDirectories: Boolean,
    forbid: List[String],
    scriptSnippetList: List[String],
    scalaSnippetList: List[String],
    javaSnippetList: List[String],
    markdownSnippetList: List[String],
    enableMarkdown: Boolean = false,
    extraClasspathWasPassed: Boolean = false
  )(using ScalaCliInvokeData): Either[BuildException, Inputs] = {
    val resourceInputs = resourceDirs
      .map(os.Path(_, Os.pwd))
      .map { path =>
        if (!os.exists(path))
          logger.message(s"WARNING: provided resource directory path doesn't exist: $path")
        path
      }
      .map(ResourceDirectory.apply)
    val maybeInputs = Inputs(
      args,
      Os.pwd,
      defaultInputs = defaultInputs,
      download = downloadInputs(cache),
      stdinOpt = readStdin(logger = logger),
      scriptSnippetList = scriptSnippetList,
      scalaSnippetList = scalaSnippetList,
      javaSnippetList = javaSnippetList,
      markdownSnippetList = markdownSnippetList,
      acceptFds = !Properties.isWin,
      forcedWorkspace = forcedWorkspaceOpt,
      enableMarkdown = enableMarkdown,
      allowRestrictedFeatures = ScalaCli.allowRestrictedFeatures,
      extraClasspathWasPassed = extraClasspathWasPassed
    )

    maybeInputs.map { inputs =>
      val forbiddenDirs =
        (if (defaultForbiddenDirectories) myDefaultForbiddenDirectories else Nil) ++
          forbid.filter(_.trim.nonEmpty).map(os.Path(_, Os.pwd))

      inputs
        .add(resourceInputs)
        .checkAttributes(directories)
        .avoid(forbiddenDirs, directories)
    }
  }

  private def readStdin(in: InputStream = System.in, logger: Logger): Option[Array[Byte]] =
    if (in == null) {
      logger.debug("No stdin available")
      None
    }
    else {
      logger.debug("Reading stdin")
      val result = in.readAllBytes()
      logger.debug(s"Done reading stdin (${result.length} B)")
      Some(result)
    }

  private def myDefaultForbiddenDirectories: Seq[os.Path] =
    if (Properties.isWin)
      Seq(os.Path("""C:\Windows\System32"""))
    else
      Nil

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

  private def resolveToolkitDependency(toolkitVersion: Option[String])
    : Seq[Positioned[AnyDependency]] =
    toolkitVersion.toList.map(Positioned.commandLine).map(Toolkit.resolveDependency)
}
