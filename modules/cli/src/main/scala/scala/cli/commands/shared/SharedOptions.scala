package scala.cli.commands.shared

import bloop.rifle.BloopRifleConfig
import caseapp.*
import caseapp.core.Arg
import caseapp.core.help.Help
import caseapp.core.util.Formatter
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import coursier.cache.FileCache
import coursier.core.Version
import coursier.util.{Artifact, Task}
import dependency.AnyDependency
import dependency.parser.DependencyParser

import java.io.{File, InputStream}
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean

import scala.build.EitherCps.{either, value}
import scala.build.Ops.EitherOptOps
import scala.build.bsp.buildtargets.ProjectName
import scala.build.compiler.{BloopCompilerMaker, ScalaCompilerMaker, SimpleScalaCompilerMaker}
import scala.build.compose.{Inputs, InputsComposer}
import scala.build.directives.DirectiveDescription
import scala.build.errors.{AmbiguousPlatformError, BuildException, ConfigDbException, Severity}
import scala.build.input.{Element, Module, ResourceDirectory, ScalaCliInvokeData}
import scala.build.interactive.Interactive
import scala.build.interactive.Interactive.{InteractiveAsk, InteractiveNop}
import scala.build.internal.util.WarningMessages
import scala.build.internal.{Constants, FetchExternalBinary, OsLibc, Util}
import scala.build.internals.ConsoleUtils.ScalaCliConsole
import scala.build.options.ScalaVersionUtil.fileWithTtl0
import scala.build.options.{BuildOptions, ComputeVersion, Platform, ScalacOpt, ShadowingSeq}
import scala.build.preprocessing.directives.ClasspathUtils.*
import scala.build.preprocessing.directives.Toolkit.maxScalaNativeWarningMsg
import scala.build.preprocessing.directives.{Python, Toolkit}
import scala.build.{compose, options as bo, *}
import scala.cli.ScalaCli
import scala.cli.commands.publish.ConfigUtil.*
import scala.cli.commands.shared.{
  HasGlobalOptions,
  ScalaJsOptions,
  ScalaNativeOptions,
  SharedOptions,
  SourceGeneratorOptions,
  SuppressWarningOptions
}
import scala.cli.commands.tags
import scala.cli.commands.util.JvmUtils
import scala.cli.commands.util.ScalacOptionsUtil.*
import scala.cli.config.Key.BooleanEntry
import scala.cli.config.{ConfigDb, Keys}
import scala.cli.launcher.PowerOptions
import scala.cli.util.ConfigDbUtils
import scala.concurrent.ExecutionContextExecutorService
import scala.concurrent.duration.*
import scala.util.Properties
import scala.util.control.NonFatal

// format: off
final case class SharedOptions(
  @Recurse
    sharedVersionOptions: SharedVersionOptions = SharedVersionOptions(),
  @Recurse
    sourceGenerator: SourceGeneratorOptions = SourceGeneratorOptions(),
  @Recurse
    suppress: SuppressWarningOptions = SuppressWarningOptions(),
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    powerOptions: PowerOptions = PowerOptions(),
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
  @Recurse
    benchmarking: BenchmarkingOptions = BenchmarkingOptions(),

  @Group(HelpGroup.Scala.toString)
  @HelpMessage(s"Set the Scala version (${Constants.defaultScalaVersion} by default)")
  @ValueDescription("version")
  @Name("S")
  @Name("scala")
  @Tag(tags.must)
    scalaVersion: Option[String] = None,
  @Group(HelpGroup.Scala.toString)
  @HelpMessage("Set the Scala binary version")
  @ValueDescription("version")
  @Hidden
  @Name("B")
  @Name("scalaBinary")
  @Name("scalaBin")
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

  @Hidden
  @Group(HelpGroup.Java.toString)
  @HelpMessage("Put project in class paths as a JAR rather than as a byte code directory")
  @Tag(tags.experimental)
    asJar: Boolean = false,

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

  @Recurse
    semanticDbOptions: SemanticDbOptions = SemanticDbOptions(),

  @Recurse
    input: SharedInputOptions = SharedInputOptions(),
  @Recurse
    helpGroups: HelpGroupOptions = HelpGroupOptions(),

  @Hidden
    strictBloopJsonCheck: Option[Boolean] = None,

  @Group(HelpGroup.Scala.toString)
  @Name("d")
  @Name("output-directory")
  @Name("destination")
  @Name("compileOutput")
  @Name("compileOut")
  @HelpMessage("Copy compilation results to output directory using either relative or absolute path")
  @ValueDescription("/example/path")
  @Tag(tags.must)
    compilationOutput: Option[String] = None,
  @Group(HelpGroup.Scala.toString)
  @HelpMessage(s"Add toolkit to classPath (not supported in Scala 2.12), 'default' version for Scala toolkit: ${Constants.toolkitDefaultVersion}, 'default' version for typelevel toolkit: ${Constants.typelevelToolkitDefaultVersion}")
  @ValueDescription("version|default")
  @Name("toolkit")
  @Tag(tags.implementation)
  @Tag(tags.inShortHelp)
    withToolkit: Option[String] = None,
  @HelpMessage("Exclude sources")
    exclude: List[String] = Nil,
  @HelpMessage("Force object wrapper for scripts")
  @Tag(tags.experimental)
    objectWrapper: Option[Boolean] = None,
) extends HasGlobalOptions {
  // format: on

  def logger: Logger = logging.logger
  override def global: GlobalOptions =
    GlobalOptions(logging = logging, globalSuppress = suppress.global, powerOptions = powerOptions)

  private def scalaJsOptions(opts: ScalaJsOptions): bo.ScalaJsOptions = {
    import opts.*
    bo.ScalaJsOptions(
      version = jsVersion,
      mode = bo.ScalaJsMode(jsMode),
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
      esVersionStr = jsEsVersion,
      noOpt = jsNoOpt,
      remapEsModuleImportMap = jsEsModuleImportMap.filter(_.trim.nonEmpty).map(os.Path(_, Os.pwd))
    )
  }

  private def linkerOptions(opts: ScalaJsOptions): bo.scalajs.ScalaJsLinkerOptions = {
    import opts.*
    bo.scalajs.ScalaJsLinkerOptions(
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

  private def scalaNativeOptions(
    opts: ScalaNativeOptions,
    maxDefaultScalaNativeVersions: List[(String, String)]
  ): bo.ScalaNativeOptions = {
    import opts.*
    bo.ScalaNativeOptions(
      version = nativeVersion,
      modeStr = nativeMode,
      ltoStr = nativeLto,
      gcStr = nativeGc,
      clang = nativeClang,
      clangpp = nativeClangpp,
      linkingOptions = nativeLinking,
      linkingDefaults = nativeLinkingDefaults,
      compileOptions = nativeCompile,
      compileDefaults = nativeCompileDefaults,
      embedResources = embedResources,
      buildTargetStr = nativeTarget,
      multithreading = nativeMultithreading,
      maxDefaultNativeVersions = maxDefaultScalaNativeVersions
    )
  }

  lazy val scalacOptionsFromFiles: List[String] =
    scalac.argsFiles.flatMap(argFile =>
      ArgSplitter.splitToArgs(os.read(os.Path(argFile.file, os.pwd)))
    )

  def scalacOptions: List[String] = scalac.scalacOption ++ scalacOptionsFromFiles

  def buildOptions(
    enableJmh: Boolean = false,
    jmhVersion: Option[String] = None,
    ignoreErrors: Boolean = false
  ): Either[BuildException, bo.BuildOptions] = either {
    val releaseOpt = scalacOptions.getScalacOption("-release")
    val targetOpt  = scalacOptions.getScalacPrefixOption("-target")
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
    val (assumedSourceJars, extraRegularJarsAndClasspath) =
      extraJarsAndClassPath.partition(_.hasSourceJarSuffix)
    if assumedSourceJars.nonEmpty then
      val assumedSourceJarsString = assumedSourceJars.mkString(", ")
      logger.message(
        s"""[${Console.YELLOW}warn${Console.RESET}] Jars with the ${ScalaCliConsole
            .GRAY}*-sources.jar${Console.RESET} name suffix are assumed to be source jars.
           |The following jars were assumed to be source jars and will be treated as such: $assumedSourceJarsString""".stripMargin
      )
    val (resolvedToolkitDependency, toolkitMaxDefaultScalaNativeVersions) =
      SharedOptions.resolveToolkitDependencyAndScalaNativeVersionReqs(withToolkit, logger)
    val scalapyMaxDefaultScalaNativeVersions =
      if sharedPython.python.contains(true) then
        List(Constants.scalaPyMaxScalaNative -> Python.maxScalaNativeWarningMsg)
      else Nil
    val maxDefaultScalaNativeVersions =
      toolkitMaxDefaultScalaNativeVersions.toList ++ scalapyMaxDefaultScalaNativeVersions
    val snOpts = scalaNativeOptions(native, maxDefaultScalaNativeVersions)
    bo.BuildOptions(
      sourceGeneratorOptions = bo.SourceGeneratorOptions(
        useBuildInfo = sourceGenerator.useBuildInfo,
        projectVersion = sharedVersionOptions.projectVersion,
        computeVersion = value {
          sharedVersionOptions.computeVersion
            .map(Positioned.commandLine)
            .map(ComputeVersion.parse)
            .sequence
        }
      ),
      suppressWarningOptions =
        bo.SuppressWarningOptions(
          suppressDirectivesInMultipleFilesWarning = getOptionOrFromConfig(
            suppress.suppressDirectivesInMultipleFilesWarning,
            Keys.suppressDirectivesInMultipleFilesWarning
          ),
          suppressOutdatedDependencyWarning = getOptionOrFromConfig(
            suppress.suppressOutdatedDependencyWarning,
            Keys.suppressOutdatedDependenciessWarning
          ),
          suppressExperimentalFeatureWarning = getOptionOrFromConfig(
            suppress.global.suppressExperimentalFeatureWarning,
            Keys.suppressExperimentalFeatureWarning
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
        semanticDbOptions = bo.SemanticDbOptions(
          generateSemanticDbs = semanticDbOptions.semanticDb,
          semanticDbTargetRoot = semanticDbOptions.semanticDbTargetRoot.map(os.Path(_, os.pwd)),
          semanticDbSourceRoot = semanticDbOptions.semanticDbSourceRoot.map(os.Path(_, os.pwd))
        ),
        scalacOptions = scalacOptions
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
        forceObjectWrapper = objectWrapper
      ),
      scalaJsOptions = scalaJsOptions(js),
      scalaNativeOptions = snOpts,
      javaOptions = value(scala.cli.commands.util.JvmUtils.javaOptions(jvm)),
      jmhOptions = bo.JmhOptions(
        addJmhDependencies =
          if (enableJmh) jmhVersion.orElse(Some(Constants.jmhVersion))
          else None,
        runJmh = if (enableJmh) Some(true) else None
      ),
      classPathOptions = bo.ClassPathOptions(
        extraClassPath = extraRegularJarsAndClasspath,
        extraCompileOnlyJars = extraCompileOnlyClassPath,
        extraSourceJars = extraSourceJars.extractedClassPath ++ assumedSourceJars,
        extraRepositories =
          (ScalaCli.launcherOptions.scalaRunner.cliPredefinedRepository ++ dependencies.repository)
            .map(_.trim)
            .filter(_.nonEmpty),
        extraDependencies = extraDependencies(ignoreErrors, resolvedToolkitDependency),
        extraCompileOnlyDependencies =
          extraCompileOnlyDependencies(ignoreErrors, resolvedToolkitDependency)
      ),
      internal = bo.InternalOptions(
        cache = Some(coursierCache),
        localRepository = LocalRepo.localRepo(Directories.directories.localRepoDir, logger),
        verbosity = Some(logging.verbosity),
        strictBloopJsonCheck = strictBloopJsonCheck,
        interactive = Some(() => interactive),
        exclude = exclude.map(Positioned.commandLine),
        offline = coursier.getOffline()
      ),
      notForBloopOptions = bo.PostBuildOptions(
        scalaJsLinkerOptions = linkerOptions(js),
        addRunnerDependencyOpt = runner,
        python = sharedPython.python,
        pythonSetup = sharedPython.pythonSetup,
        scalaPyVersion = sharedPython.scalaPyVersion
      ),
      useBuildServer = compilationServer.server
    )
  }

  private def resolvedDependencies(
    deps: List[String],
    ignoreErrors: Boolean,
    extraResolvedDependencies: Seq[Positioned[AnyDependency]]
  ) = ShadowingSeq.from {
    SharedOptions.parseDependencies(deps.map(Positioned.none), ignoreErrors) ++
      extraResolvedDependencies
  }

  private def extraCompileOnlyDependencies(
    ignoreErrors: Boolean,
    resolvedDeps: Seq[Positioned[AnyDependency]]
  ) = {
    val jmhCorePrefix = s"${Constants.jmhOrg}:${Constants.jmhCoreModule}"
    val jmhDeps =
      if benchmarking.jmh.getOrElse(false) &&
        !dependencies.compileOnlyDependency.exists(_.startsWith(jmhCorePrefix)) &&
        !dependencies.dependency.exists(_.startsWith(jmhCorePrefix))
      then List(s"$jmhCorePrefix:${Constants.jmhVersion}")
      else List.empty
    val finalDeps = dependencies.compileOnlyDependency ++ jmhDeps
    resolvedDependencies(finalDeps, ignoreErrors, resolvedDeps)
  }

  private def extraDependencies(
    ignoreErrors: Boolean,
    resolvedDeps: Seq[Positioned[AnyDependency]]
  ) = resolvedDependencies(dependencies.dependency, ignoreErrors, resolvedDeps)

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
    (extraJars ++ scalacOptions.getScalacOption("-classpath") ++ scalacOptions.getScalacOption(
      "-cp"
    ))
      .extractedClassPath

  def extraClasspathWasPassed: Boolean =
    extraJarsAndClassPath.exists(!_.hasSourceJarSuffix) || dependencies.dependency.nonEmpty

  def extraCompileOnlyClassPath: List[os.Path] = extraCompileOnlyJars.extractedClassPath

  def globalInteractiveWasSuggested: Either[BuildException, Option[Boolean]] = either {
    value(ConfigDbUtils.configDb).get(Keys.globalInteractiveWasSuggested) match {
      case Right(opt) => opt
      case Left(ex) =>
        logger.debug(ConfigDbException(ex))
        None
    }
  }

  def interactive: Either[BuildException, Interactive] = either {
    (
      logging.verbosityOptions.interactive,
      value(ConfigDbUtils.configDb).get(Keys.interactive) match {
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
            val configDb0 = value(ConfigDbUtils.configDb)
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
            val configDb0 = value(ConfigDbUtils.configDb)
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
      ConfigDbUtils.configDb.map(_.get(configDbKey))
        .map {
          case Right(opt) => opt
          case Left(ex) =>
            logger.debug(ConfigDbException(ex))
            None
        }
        .getOrElse(None)
    )

  def bloopRifleConfig(extraBuildOptions: Option[BuildOptions] = None)
    : Either[BuildException, BloopRifleConfig] = either {
    val options = extraBuildOptions.foldLeft(value(buildOptions(false, None)))(_ orElse _)
    lazy val defaultJvmHome = value {
      JvmUtils.downloadJvm(OsLibc.defaultJvm(OsLibc.jvmIndexOs), options)
    }

    val javaHomeInfo = compilationServer.bloopJvm
      .map(jvmId => value(JvmUtils.downloadJvm(jvmId, options)))
      .orElse {
        for (javaHome <- options.javaHomeLocationOpt()) yield {
          val (javaHomeVersion, javaHomeCmd) = OsLibc.javaHomeVersion(javaHome.value)
          if (javaHomeVersion >= Constants.minimumBloopJavaVersion)
            BuildOptions.JavaHomeInfo(javaHome.value, javaHomeCmd, javaHomeVersion)
          else defaultJvmHome
        }
      }.getOrElse(defaultJvmHome)

    compilationServer.bloopRifleConfig(
      logging.logger,
      coursierCache,
      logging.verbosity,
      javaHomeInfo.javaCommand,
      Directories.directories,
      Some(javaHomeInfo.version)
    )
  }

  def compilerMaker(
    threads: BuildThreads,
    scaladoc: Boolean = false
  ): ScalaCompilerMaker =
    if (scaladoc)
      SimpleScalaCompilerMaker("java", Nil, scaladoc = true)
    else if (compilationServer.server.getOrElse(true))
      new BloopCompilerMaker(
        options => bloopRifleConfig(Some(options)),
        threads.bloop,
        strictBloopJsonCheckOrDefault,
        coursier.getOffline().getOrElse(false)
      )
    else SimpleScalaCompilerMaker("java", Nil)

  lazy val coursierCache = coursier.coursierCache(logging.logger.coursierLogger(""))

  private def moduleInputsFromArgs(
    args: Seq[String],
    forcedProjectName: Option[ProjectName],
    defaultInputs: () => Option[Module] = () => Module.default()
  )(using ScalaCliInvokeData) = SharedOptions.inputs(
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
    extraClasspathWasPassed = extraClasspathWasPassed,
    forcedProjectName = forcedProjectName
  )

  def composeInputs(
    args: Seq[String],
    defaultInputs: () => Option[Module] = () => Module.default()
  )(using ScalaCliInvokeData): Either[BuildException, Inputs] = {
    val updatedModuleInputsFromArgs
      : (Seq[String], Option[ProjectName]) => Either[BuildException, Module] =
      (args, projectNameOpt) =>
        for {
          moduleInputs <- moduleInputsFromArgs(args, projectNameOpt, defaultInputs)
          options      <- buildOptions()
        } yield Build.updateInputs(moduleInputs, options)

    InputsComposer(
      args,
      Os.pwd,
      updatedModuleInputsFromArgs,
      ScalaCli.allowRestrictedFeatures
    ).getInputs
  }

  def inputs(
    args: Seq[String],
    defaultInputs: () => Option[Module] = () => Module.default()
  )(using ScalaCliInvokeData) = moduleInputsFromArgs(args, forcedProjectName = None, defaultInputs)

  def allScriptSnippets: List[String]   = snippet.scriptSnippet ++ snippet.executeScript
  def allScalaSnippets: List[String]    = snippet.scalaSnippet ++ snippet.executeScala
  def allJavaSnippets: List[String]     = snippet.javaSnippet ++ snippet.executeJava
  def allMarkdownSnippets: List[String] = snippet.markdownSnippet ++ snippet.executeMarkdown

  def hasSnippets =
    allScriptSnippets.nonEmpty || allScalaSnippets.nonEmpty || allJavaSnippets
      .nonEmpty || allMarkdownSnippets.nonEmpty

  def validateInputArgs(
    args: Seq[String]
  )(using ScalaCliInvokeData): Seq[Either[String, Seq[Element]]] =
    Module.validateArgs(
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
  import ArgFileOption.parser
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

  /** [[Module]] builder, handy when you don't have a [[SharedOptions]] instance at hand */
  def inputs(
    args: Seq[String],
    defaultInputs: () => Option[Module],
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
    extraClasspathWasPassed: Boolean = false,
    forcedProjectName: Option[ProjectName] = None
  )(using ScalaCliInvokeData): Either[BuildException, Module] = {
    val resourceInputs = resourceDirs
      .map(os.Path(_, Os.pwd))
      .map { path =>
        if (!os.exists(path))
          logger.message(s"WARNING: provided resource directory path doesn't exist: $path")
        path
      }
      .map(ResourceDirectory.apply)

    val maybeInputs = Module(
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
      extraClasspathWasPassed = extraClasspathWasPassed,
      forcedProjectName = forcedProjectName
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

  // TODO: remove this state after resolving https://github.com/VirtusLab/scala-cli/issues/2658
  private val loggedDeprecatedToolkitWarning: AtomicBoolean = AtomicBoolean(false)
  private def resolveToolkitDependencyAndScalaNativeVersionReqs(
    toolkitVersion: Option[String],
    logger: Logger
  ): (Seq[Positioned[AnyDependency]], Seq[(String, String)]) = {
    if (
      (toolkitVersion.contains("latest")
      || toolkitVersion.contains(Toolkit.typelevel + ":latest")
      || toolkitVersion.contains(
        Constants.typelevelOrganization + ":latest"
      )) && !loggedDeprecatedToolkitWarning.getAndSet(true)
    ) logger.message(
      WarningMessages.deprecatedToolkitLatest(
        s"--toolkit ${toolkitVersion.map(_.replace("latest", "default")).getOrElse("default")}"
      )
    )

    val (dependencies, toolkitDefinitions) =
      toolkitVersion.toList.map(Positioned.commandLine)
        .flatMap(Toolkit.resolveDependenciesWithRequirements(_).map((wbr, td) => wbr.value -> td))
        .unzip
    val maxScalaNativeVersions =
      toolkitDefinitions.flatMap {
        case Toolkit.ToolkitDefinitions(
              isScalaToolkitDefault,
              explicitScalaToolkitVersion,
              isTypelevelToolkitDefault,
              _
            ) =>
          val st = if (isScalaToolkitDefault)
            Seq(Constants.toolkitMaxScalaNative -> Toolkit.maxScalaNativeWarningMsg(
              toolkitName = "Scala Toolkit",
              toolkitVersion = Constants.toolkitDefaultVersion,
              maxNative = Constants.toolkitMaxScalaNative
            ))
          else explicitScalaToolkitVersion.toList
            .map(Version(_))
            .filter(_ <= Version(Constants.toolkitVersionForNative04))
            .flatMap(v =>
              List(Constants.scalaNativeVersion04 -> maxScalaNativeWarningMsg(
                toolkitName = "Scala Toolkit",
                toolkitVersion = v.toString(),
                Constants.scalaNativeVersion04
              ))
            )
          val tlt =
            if (isTypelevelToolkitDefault)
              Seq(Constants.typelevelToolkitMaxScalaNative -> Toolkit.maxScalaNativeWarningMsg(
                toolkitName = "TypeLevel Toolkit",
                toolkitVersion = Constants.typelevelToolkitDefaultVersion,
                maxNative = Constants.typelevelToolkitMaxScalaNative
              ))
            else Nil
          st ++ tlt
      }
    dependencies -> maxScalaNativeVersions
  }
}
