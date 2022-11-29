package scala.cli.commands.repl

import ai.kien.python.Python
import caseapp.*
import coursier.cache.FileCache
import coursier.error.{FetchError, ResolutionError}
import dependency.*

import scala.build.EitherCps.{either, value}
import scala.build.*
import scala.build.errors.{BuildException, CantDownloadAmmoniteError, FetchingDependenciesError}
import scala.build.input.Inputs
import scala.build.internal.{Constants, Runner}
import scala.build.options.{BuildOptions, JavaOpt, MaybeScalaVersion, Scope}
import scala.cli.CurrentParams
import scala.cli.commands.publish.ConfigUtil.*
import scala.cli.commands.run.Run.{maybePrintSimpleScalacOutput, orPythonDetectionError}
import scala.cli.commands.run.RunMode
import scala.cli.commands.shared.SharedOptions
import scala.cli.commands.{ScalaCommand, WatchUtil}
import scala.cli.config.{ConfigDb, Keys}
import scala.util.Properties

object Repl extends ScalaCommand[ReplOptions] {
  override def group                   = "Main"
  override def scalaSpecificationLevel = SpecificationLevel.MUST
  override def names: List[List[String]] = List(
    List("repl"),
    List("console")
  )
  override def sharedOptions(options: ReplOptions): Option[SharedOptions] = Some(options.shared)

  override def buildOptions(ops: ReplOptions): Some[BuildOptions] =
    Some(buildOptions0(ops, scala.cli.internal.Constants.maxAmmoniteScala3Version))
  private[commands] def buildOptions0(
    ops: ReplOptions,
    maxAmmoniteScalaVer: String
  ): BuildOptions = {
    import ops.*
    import ops.sharedRepl.*

    val logger = ops.shared.logger

    val ammoniteVersionOpt = ammoniteVersion.map(_.trim).filter(_.nonEmpty)
    val baseOptions        = shared.buildOptions().orExit(logger)
    baseOptions.copy(
      scalaOptions = baseOptions.scalaOptions.copy(
        scalaVersion = baseOptions.scalaOptions.scalaVersion
          .orElse {
            val defaultScalaVer = scala.build.internal.Constants.defaultScalaVersion
            val shouldDowngrade = {
              def needsDowngradeForAmmonite = {
                import coursier.core.Version
                Version(maxAmmoniteScalaVer) < Version(defaultScalaVer)
              }
              ammonite.contains(true) &&
              ammoniteVersionOpt.isEmpty &&
              needsDowngradeForAmmonite
            }
            if (shouldDowngrade) {
              logger.message(
                s"Scala $defaultScalaVer is not yet supported with this version of Ammonite"
              )
              logger.message(s"Defaulting to Scala $maxAmmoniteScalaVer")
              Some(MaybeScalaVersion(maxAmmoniteScalaVer))
            }
            else None
          }
      ),
      javaOptions = baseOptions.javaOptions.copy(
        javaOpts =
          baseOptions.javaOptions.javaOpts ++
            sharedJava.allJavaOpts.map(JavaOpt(_)).map(Positioned.commandLine)
      ),
      notForBloopOptions = baseOptions.notForBloopOptions.copy(
        replOptions = baseOptions.notForBloopOptions.replOptions.copy(
          useAmmoniteOpt = ammonite,
          ammoniteVersionOpt = ammoniteVersion.map(_.trim).filter(_.nonEmpty),
          ammoniteArgs = ammoniteArg
        ),
        addRunnerDependencyOpt = baseOptions.notForBloopOptions.addRunnerDependencyOpt
          .orElse(Some(false))
      )
    )
  }

  private def runMode(options: ReplOptions): RunMode.HasRepl =
    RunMode.Default

  override def runCommand(options: ReplOptions, args: RemainingArgs, logger: Logger): Unit = {
    val initialBuildOptions = buildOptionsOrExit(options)
    def default = Inputs.default().getOrElse {
      Inputs.empty(Os.pwd, options.shared.markdown.enableMarkdown)
    }
    val inputs =
      options.shared.inputs(args.remaining, defaultInputs = () => Some(default)).orExit(logger)
    val programArgs = args.unparsed
    CurrentParams.workspaceOpt = Some(inputs.workspace)

    val threads = BuildThreads.create()

    val compilerMaker = options.shared.compilerMaker(threads).orExit(logger)

    val directories = options.shared.directories.directories

    def buildFailed(allowExit: Boolean): Unit = {
      System.err.println("Compilation failed")
      if (allowExit)
        sys.exit(1)
    }
    def buildCancelled(allowExit: Boolean): Unit = {
      System.err.println("Build cancelled")
      if (allowExit)
        sys.exit(1)
    }

    def doRunRepl(
      buildOptions: BuildOptions,
      artifacts: Artifacts,
      classDir: Option[os.Path],
      allowExit: Boolean,
      runMode: RunMode.HasRepl,
      buildOpt: Option[Build.Successful]
    ): Unit = {
      val res = runRepl(
        buildOptions,
        programArgs,
        artifacts,
        classDir,
        directories,
        logger,
        allowExit = allowExit,
        options.sharedRepl.replDryRun,
        runMode,
        buildOpt
      )
      res match {
        case Left(ex) =>
          if (allowExit) logger.exit(ex)
          else logger.log(ex)
        case Right(()) =>
      }
    }
    def doRunReplFromBuild(
      build: Build.Successful,
      allowExit: Boolean,
      runMode: RunMode.HasRepl
    ): Unit =
      doRunRepl(
        build.options,
        build.artifacts,
        build.outputOpt,
        allowExit,
        runMode,
        Some(build)
      )

    val cross    = options.sharedRepl.compileCross.cross.getOrElse(false)
    val configDb = options.shared.configDb.orExit(logger)
    val actionableDiagnostics =
      options.shared.logging.verbosityOptions.actions.orElse(
        configDb.get(Keys.actions).getOrElse(None)
      )

    if (inputs.isEmpty) {
      val artifacts = initialBuildOptions.artifacts(logger, Scope.Main).orExit(logger)
      // synchronizing, so that multiple presses to enter (handled by WatchUtil.waitForCtrlC)
      // don't try to run repls in parallel
      val lock = new Object
      def runThing() = lock.synchronized {
        doRunRepl(
          initialBuildOptions,
          artifacts,
          None,
          allowExit = !options.sharedRepl.watch.watchMode,
          runMode = runMode(options),
          buildOpt = None
        )
      }
      runThing()
      if (options.sharedRepl.watch.watchMode) {
        // nothing to watch, just wait for Ctrl+C
        WatchUtil.printWatchMessage()
        WatchUtil.waitForCtrlC(() => runThing())
      }
    }
    else if (options.sharedRepl.watch.watchMode) {
      val watcher = Build.watch(
        inputs,
        initialBuildOptions,
        compilerMaker,
        None,
        logger,
        crossBuilds = cross,
        buildTests = false,
        partial = None,
        actionableDiagnostics = actionableDiagnostics,
        postAction = () => WatchUtil.printWatchMessage()
      ) { res =>
        for (builds <- res.orReport(logger))
          builds.main match {
            case s: Build.Successful =>
              doRunReplFromBuild(s, allowExit = false, runMode = runMode(options))
            case _: Build.Failed    => buildFailed(allowExit = false)
            case _: Build.Cancelled => buildCancelled(allowExit = false)
          }
      }
      try WatchUtil.waitForCtrlC(() => watcher.schedule())
      finally watcher.dispose()
    }
    else {
      val builds =
        Build.build(
          inputs,
          initialBuildOptions,
          compilerMaker,
          None,
          logger,
          crossBuilds = cross,
          buildTests = false,
          partial = None,
          actionableDiagnostics = actionableDiagnostics
        )
          .orExit(logger)
      builds.main match {
        case s: Build.Successful =>
          doRunReplFromBuild(s, allowExit = true, runMode = runMode(options))
        case _: Build.Failed    => buildFailed(allowExit = true)
        case _: Build.Cancelled => buildCancelled(allowExit = true)
      }
    }
  }

  private def maybeAdaptForWindows(args: Seq[String]): Seq[String] =
    if (Properties.isWin)
      args.map { a =>
        if (a.contains(" ")) "\"" + a.replace("\"", "\\\"") + "\""
        else a
      }
    else
      args

  private def runRepl(
    options: BuildOptions,
    programArgs: Seq[String],
    artifacts: Artifacts,
    classDir: Option[os.Path],
    directories: scala.build.Directories,
    logger: Logger,
    allowExit: Boolean,
    dryRun: Boolean,
    runMode: RunMode.HasRepl,
    buildOpt: Option[Build.Successful]
  ): Either[BuildException, Unit] = either {

    val setupPython = options.notForBloopOptions.python.getOrElse(false)

    val cache             = options.internal.cache.getOrElse(FileCache())
    val shouldUseAmmonite = options.notForBloopOptions.replOptions.useAmmonite

    val scalaParams = artifacts.scalaOpt
      .getOrElse {
        sys.error("Expected Scala artifacts to be fetched")
      }
      .params

    val scalapyJavaOpts =
      if (setupPython) {
        val props = value {
          val python       = Python()
          val propsOrError = python.scalapyProperties
          logger.debug(s"Python Java properties: $propsOrError")
          propsOrError.orPythonDetectionError
        }
        props.toVector.sorted.map {
          case (k, v) => s"-D$k=$v"
        }
      }
      else
        Nil

    def additionalArgs = {
      val pythonArgs =
        if (setupPython && scalaParams.scalaVersion.startsWith("2.13."))
          Seq("-Yimports:java.lang,scala,scala.Predef,me.shadaj.scalapy")
        else
          Nil
      pythonArgs ++ options.scalaOptions.scalacOptions.toSeq.map(_.value.value)
    }

    def ammoniteAdditionalArgs() = {
      val pythonPredef =
        if (setupPython)
          """import me.shadaj.scalapy.py
            |import me.shadaj.scalapy.py.PyQuote
            |""".stripMargin
        else
          ""
      val predefArgs =
        if (pythonPredef.isEmpty) Nil
        else Seq("--predef-code", pythonPredef)
      predefArgs ++ options.notForBloopOptions.replOptions.ammoniteArgs
    }

    // TODO Warn if some entries of artifacts.classPath were evicted in replArtifacts.replClassPath
    //      (should be artifacts whose version was bumped by Ammonite).

    // TODO Find the common namespace of all user classes, and import it all in the Ammonite session.

    // TODO Allow to disable printing the welcome banner and the "Loading..." message in Ammonite.

    val rootClasses = classDir
      .toSeq
      .flatMap(os.list(_))
      .filter(_.last.endsWith(".class"))
      .filter(os.isFile(_)) // just in case
      .map(_.last.stripSuffix(".class"))
      .sorted
    val warnRootClasses = rootClasses.nonEmpty &&
      options.notForBloopOptions.replOptions.useAmmoniteOpt.contains(true)
    if (warnRootClasses)
      logger.message(
        s"Warning: found classes defined in the root package (${rootClasses.mkString(", ")})." +
          " These will not be accessible from the REPL."
      )

    def actualBuild: Build.Successful =
      buildOpt.getOrElse {
        val ws      = os.temp.dir()
        val inputs  = Inputs.empty(ws, enableMarkdown = false)
        val sources = Sources(Nil, Nil, None, Nil, options)
        val scope   = Scope.Main
        Build.Successful(
          inputs = inputs,
          options = options,
          scalaParams = Some(scalaParams),
          scope = scope,
          sources = Sources(Nil, Nil, None, Nil, options),
          artifacts = artifacts,
          project = value(Build.buildProject(inputs, sources, Nil, options, None, scope, logger)),
          output = classDir.getOrElse(ws),
          diagnostics = None,
          generatedSources = Nil,
          isPartial = false
        )
      }

    def maybeRunRepl(
      replArtifacts: ReplArtifacts,
      replArgs: Seq[String],
      extraEnv: Map[String, String] = Map.empty,
      extraProps: Map[String, String] = Map.empty
    ): Unit =
      if (dryRun)
        logger.message("Dry run, not running REPL.")
      else {
        val retCode = Runner.runJvm(
          options.javaHome().value.javaCommand,
          scalapyJavaOpts ++
            replArtifacts.replJavaOpts ++
            options.javaOptions.javaOpts.toSeq.map(_.value.value) ++
            extraProps.toVector.sorted.map { case (k, v) => s"-D$k=$v" },
          classDir.toSeq ++ replArtifacts.replClassPath,
          replArtifacts.replMainClass,
          maybeAdaptForWindows(replArgs),
          logger,
          allowExecve = allowExit,
          extraEnv = extraEnv
        ).waitFor()
        if (retCode != 0)
          value(Left(new ReplError(retCode)))
      }

    def defaultArtifacts(): Either[BuildException, ReplArtifacts] =
      ReplArtifacts.default(
        scalaParams,
        artifacts.userDependencies,
        artifacts.extraClassPath,
        logger,
        cache,
        options.finalRepositories,
        addScalapy =
          if (setupPython)
            Some(options.notForBloopOptions.scalaPyVersion.getOrElse(Constants.scalaPyVersion))
          else None
      )
    def ammoniteArtifacts(): Either[BuildException, ReplArtifacts] =
      ReplArtifacts.ammonite(
        scalaParams,
        options.notForBloopOptions.replOptions.ammoniteVersion,
        artifacts.userDependencies,
        artifacts.extraClassPath,
        artifacts.extraSourceJars,
        logger,
        cache,
        directories,
        addScalapy =
          if (setupPython)
            Some(options.notForBloopOptions.scalaPyVersion.getOrElse(Constants.scalaPyVersion))
          else None
      ).left.map {
        case FetchingDependenciesError(e: ResolutionError.CantDownloadModule, positions)
            if shouldUseAmmonite && e.module.name.value == s"ammonite_${scalaParams.scalaVersion}" =>
          CantDownloadAmmoniteError(e.version, scalaParams.scalaVersion, e, positions)
        case other => other
      }

    if (shouldUseAmmonite)
      runMode match {
        case RunMode.Default =>
          val replArtifacts = value(ammoniteArtifacts())
          val replArgs      = ammoniteAdditionalArgs() ++ programArgs
          maybeRunRepl(replArtifacts, replArgs)
      }
    else
      runMode match {
        case RunMode.Default =>
          val replArtifacts = value(defaultArtifacts())
          val replArgs      = additionalArgs ++ programArgs
          maybeRunRepl(replArtifacts, replArgs)
      }
  }

  final class ReplError(retCode: Int)
      extends BuildException(s"Failed to run REPL (exit code: $retCode)")
}
