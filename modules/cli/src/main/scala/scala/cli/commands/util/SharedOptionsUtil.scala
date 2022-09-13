package scala.cli.commands
package util

import caseapp.RemainingArgs
import coursier.cache.FileCache
import coursier.util.{Artifact, Task}
import dependency.AnyDependency
import dependency.parser.DependencyParser

import java.io.{File, InputStream}

import scala.build.*
import scala.build.blooprifle.BloopRifleConfig
import scala.build.compiler.{BloopCompilerMaker, ScalaCompilerMaker, SimpleScalaCompilerMaker}
import scala.build.errors.BuildException
import scala.build.interactive.Interactive
import scala.build.interactive.Interactive.{InteractiveAsk, InteractiveNop}
import scala.build.internal.CsLoggerUtil.*
import scala.build.internal.{Constants, FetchExternalBinary, OsLibc, Util}
import scala.build.options.{Platform, ScalacOpt, ShadowingSeq}
import scala.build.options as bo
import scala.cli.ScalaCli
import scala.cli.commands.ScalaJsOptions
import scala.cli.commands.util.CommonOps.*
import scala.cli.commands.util.SharedCompilationServerOptionsUtil.*
import scala.cli.config.{ConfigDb, Keys}
import scala.concurrent.ExecutionContextExecutorService
import scala.concurrent.duration.*
import scala.util.Properties
import scala.util.control.NonFatal

object SharedOptionsUtil extends CommandHelpers {

  private def downloadInputs(cache: FileCache[Task]): String => Either[String, Array[Byte]] = {
    url =>
      val artifact = Artifact(url).withChanging(true)
      val res = cache.logger.use {
        try cache.withTtl(0.seconds).file(artifact).run.unsafeRun()(cache.ec)
        catch {
          case NonFatal(e) => throw new Exception(e)
        }
      }
      res
        .left.map(_.describe)
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
    enableMarkdown: Boolean = false
  ): Either[BuildException, Inputs] = {
    val resourceInputs = resourceDirs
      .map(os.Path(_, Os.pwd))
      .map { path =>
        if (!os.exists(path))
          logger.message(s"WARNING: provided resource directory path doesn't exist: $path")
        path
      }
      .map(Inputs.ResourceDirectory.apply)
    val maybeInputs = Inputs(
      args,
      Os.pwd,
      directories,
      defaultInputs = defaultInputs,
      download = downloadInputs(cache),
      stdinOpt = readStdin(logger = logger),
      scriptSnippetList = scriptSnippetList,
      scalaSnippetList = scalaSnippetList,
      javaSnippetList = javaSnippetList,
      acceptFds = !Properties.isWin,
      forcedWorkspace = forcedWorkspaceOpt,
      enableMarkdown = enableMarkdown,
      allowRestrictedFeatures = ScalaCli.allowRestrictedFeatures
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

  implicit class SharedOptionsOps(v: SharedOptions) {
    import v._

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
    ): bo.BuildOptions = {
      val platformOpt =
        if (js.js) Some(Platform.JS)
        else if (native.native) Some(Platform.Native)
        else None
      bo.BuildOptions(
        scalaOptions = bo.ScalaOptions(
          scalaVersion = scalaVersion
            .map(_.trim)
            .filter(_.nonEmpty)
            .map(bo.MaybeScalaVersion(_)),
          scalaBinaryVersion = scalaBinaryVersion.map(_.trim).filter(_.nonEmpty),
          addScalaLibrary = scalaLibrary.orElse(java.map(!_)),
          generateSemanticDbs = semanticDb,
          scalacOptions = ShadowingSeq.from(
            scalac.scalacOption
              .filter(_.nonEmpty)
              .map(ScalacOpt(_))
              .map(Positioned.commandLine)
          ),
          compilerPlugins =
            SharedOptionsUtil.parseDependencies(
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
        javaOptions = scala.cli.commands.util.JvmUtils.javaOptions(jvm),
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
            SharedOptionsUtil.parseDependencies(
              dependencies.dependency.map(Positioned.none),
              ignoreErrors
            )
          )
        ),
        internal = bo.InternalOptions(
          cache = Some(coursierCache),
          localRepository = LocalRepo.localRepo(directories.directories.localRepoDir),
          verbosity = Some(logging.verbosity),
          strictBloopJsonCheck = strictBloopJsonCheck,
          interactive = Some(() => interactive)
        ),
        notForBloopOptions = bo.PostBuildOptions(
          scalaJsLinkerOptions = linkerOptions(js)
        )
      )
    }

    def globalInteractiveWasSuggested: Option[Boolean] =
      configDb.getOrNone(Keys.globalInteractiveWasSuggested, logger)

    def interactive: Interactive =
      (
        logging.verbosityOptions.interactive,
        configDb.getOrNone(Keys.interactive, logger),
        globalInteractiveWasSuggested
      ) match {
        case (Some(true), _, Some(true)) => InteractiveAsk
        case (_, Some(true), _)          => InteractiveAsk
        case (Some(true), _, _) =>
          val answers @ List(yesAnswer, _) = List("Yes", "No")
          InteractiveAsk.chooseOne(
            """You have run the current scala-cli command with the --interactive mode turned on.
              |Would you like to leave it on permanently?""".stripMargin,
            answers
          ) match {
            case Some(answer) if answer == yesAnswer =>
              configDb
                .set(Keys.interactive, true)
                .set(Keys.globalInteractiveWasSuggested, true)
                .save(v.directories.directories)
              logger.message(
                "--interactive is now set permanently. All future scala-cli commands will run with the flag set to true."
              )
              logger.message(
                "If you want to turn this setting off at any point, just run `scala-cli config interactive false`."
              )
            case _ =>
              configDb
                .set(Keys.globalInteractiveWasSuggested, true)
                .save(v.directories.directories)
              logger.message(
                "If you want to turn this setting permanently on at any point, just run `scala-cli config interactive true`."
              )
          }
          InteractiveAsk
        case _ => InteractiveNop
      }

    def configDb: ConfigDb = ConfigDb.open(v).orExit(logger)

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

    def compilerMaker(threads: BuildThreads, scaladoc: Boolean = false): ScalaCompilerMaker =
      if (scaladoc)
        SimpleScalaCompilerMaker("java", Nil, scaladoc = true)
      else if (compilationServer.server.getOrElse(true))
        new BloopCompilerMaker(
          bloopRifleConfig(),
          threads.bloop,
          strictBloopJsonCheckOrDefault
        )
      else
        SimpleScalaCompilerMaker("java", Nil)

    def coursierCache = cached(v)(coursier.coursierCache(logging.logger.coursierLogger("")))

    def inputs(
      args: Seq[String],
      defaultInputs: () => Option[Inputs] = () => Inputs.default()
    ): Either[BuildException, Inputs] =
      SharedOptionsUtil.inputs(
        args,
        defaultInputs,
        resourceDirs,
        directories.directories,
        logger = logger,
        coursierCache,
        workspace.forcedWorkspaceOpt,
        input.defaultForbiddenDirectories,
        input.forbid,
        scriptSnippetList = allScriptSnippets,
        scalaSnippetList = allScalaSnippets,
        javaSnippetList = allJavaSnippets,
        enableMarkdown = v.markdown.enableMarkdown
      )

    def allScriptSnippets: List[String] = v.snippet.scriptSnippet ++ v.snippet.executeScript
    def allScalaSnippets: List[String]  = v.snippet.scalaSnippet ++ v.snippet.executeScala
    def allJavaSnippets: List[String]   = v.snippet.javaSnippet ++ v.snippet.executeJava

    def validateInputArgs(args: Seq[String]): Seq[Either[String, Seq[Inputs.Element]]] =
      Inputs.validateArgs(
        args,
        Os.pwd,
        downloadInputs(coursierCache),
        readStdin(logger = logger),
        !Properties.isWin
      )

    def strictBloopJsonCheckOrDefault: Boolean =
      strictBloopJsonCheck.getOrElse(bo.InternalOptions.defaultStrictBloopJsonCheck)
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
}
