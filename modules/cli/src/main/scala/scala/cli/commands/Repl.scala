package scala.cli.commands

import caseapp._
import coursier.cache.FileCache
import coursier.error.{FetchError, ResolutionError}

import scala.build.EitherCps.{either, value}
import scala.build._
import scala.build.errors.{BuildException, FetchingDependenciesError, CantDownloadAmmoniteError}
import scala.build.internal.Runner
import scala.build.options.{BuildOptions, JavaOpt, Scope}
import scala.cli.CurrentParams
import scala.cli.commands.Run.maybePrintSimpleScalacOutput
import scala.cli.commands.util.CommonOps._
import scala.cli.commands.util.SharedOptionsUtil._
import scala.util.Properties
import scala.cli.config.{ConfigDb, Keys}
import scala.cli.commands.util.CommonOps.SharedDirectoriesOptionsOps

object Repl extends ScalaCommand[ReplOptions] {
  override def group = "Main"
  override def names: List[List[String]] = List(
    List("repl"),
    List("console")
  )
  override def sharedOptions(options: ReplOptions): Option[SharedOptions] = Some(options.shared)

  def buildOptions(ops: ReplOptions): BuildOptions = {
    import ops._
    import ops.sharedRepl._
    val ammoniteVersionOpt = ammoniteVersion.map(_.trim).filter(_.nonEmpty)

    val baseOptions = shared.copy(scalaVersion =
      if (
        ammonite.contains(true) &&
        (shared.scalaVersion.isEmpty || shared.scalaVersion.contains("3.2.0")) &&
        ammoniteVersionOpt.isEmpty
      ) {
        // TODO remove this once ammonite adds support for 3.2.0
        System.err.println("Scala 3.2.0 is not yet supported with this version of ammonite")
        System.err.println("Defaulting to Scala 3.1.3")
        Some("3.1.3")
      }
      else shared.scalaVersion
    ).buildOptions()
    baseOptions.copy(
      javaOptions = baseOptions.javaOptions.copy(
        javaOpts =
          baseOptions.javaOptions.javaOpts ++
            sharedJava.allJavaOpts.map(JavaOpt(_)).map(Positioned.commandLine)
      ),
      notForBloopOptions = baseOptions.notForBloopOptions.copy(
        replOptions = baseOptions.notForBloopOptions.replOptions.copy(
          useAmmoniteOpt = ammonite,
          ammoniteVersionOpt = ammoniteVersionOpt,
          ammoniteArgs = ammoniteArg
        )
      ),
      internalDependencies = baseOptions.internalDependencies.copy(
        addRunnerDependencyOpt = baseOptions.internalDependencies.addRunnerDependencyOpt
          .orElse(Some(false))
      )
    )
  }

  def run(options: ReplOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.shared.logging.verbosity
    def default = Inputs.default().getOrElse {
      Inputs.empty(Os.pwd, options.shared.markdown.enableMarkdown)
    }
    val logger = options.shared.logger
    val inputs = options.shared.inputs(args.all, defaultInputs = () => Some(default)).orExit(logger)
    val programArgs = args.unparsed
    CurrentParams.workspaceOpt = Some(inputs.workspace)

    val initialBuildOptions = buildOptions(options)
    maybePrintSimpleScalacOutput(options, initialBuildOptions)

    val threads = BuildThreads.create()

    val compilerMaker = options.shared.compilerMaker(threads)

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
      allowExit: Boolean
    ): Unit = {
      val res = runRepl(
        buildOptions,
        programArgs,
        artifacts,
        classDir,
        directories,
        logger,
        allowExit = allowExit,
        options.sharedRepl.replDryRun
      )
      res match {
        case Left(ex) =>
          if (allowExit) logger.exit(ex)
          else logger.log(ex)
        case Right(()) =>
      }
    }

    val cross = options.sharedRepl.compileCross.cross.getOrElse(false)
    val configDb = ConfigDb.open(options.shared.directories.directories)
      .orExit(logger)
    val actionableDiagnostics =
      options.shared.logging.verbosityOptions.actions.orElse(
        configDb.get(Keys.actions).getOrElse(None)
      )

    if (inputs.isEmpty) {
      val artifacts = initialBuildOptions.artifacts(logger, Scope.Main).orExit(logger)
      doRunRepl(
        initialBuildOptions,
        artifacts,
        None,
        allowExit = !options.sharedRepl.watch.watchMode
      )
      if (options.sharedRepl.watch.watchMode) {
        // nothing to watch, just wait for Ctrl+C
        WatchUtil.printWatchMessage()
        WatchUtil.waitForCtrlC()
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
              doRunRepl(s.options, s.artifacts, s.outputOpt, allowExit = false)
            case _: Build.Failed    => buildFailed(allowExit = false)
            case _: Build.Cancelled => buildCancelled(allowExit = false)
          }
      }
      try WatchUtil.waitForCtrlC()
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
          doRunRepl(s.options, s.artifacts, s.outputOpt, allowExit = true)
        case _: Build.Failed    => buildFailed(allowExit = true)
        case _: Build.Cancelled => buildCancelled(allowExit = true)
      }
    }
  }

  private def runRepl(
    options: BuildOptions,
    programArgs: Seq[String],
    artifacts: Artifacts,
    classDir: Option[os.Path],
    directories: scala.build.Directories,
    logger: Logger,
    allowExit: Boolean,
    dryRun: Boolean
  ): Either[BuildException, Unit] = either {

    val cache             = options.internal.cache.getOrElse(FileCache())
    val shouldUseAmmonite = options.notForBloopOptions.replOptions.useAmmonite
    val replArtifacts = value {
      val scalaParams = artifacts.scalaOpt
        .getOrElse {
          sys.error("Expected Scala artifacts to be fetched")
        }
        .params
      val maybeReplArtifacts =
        if (shouldUseAmmonite)
          ReplArtifacts.ammonite(
            scalaParams,
            options.notForBloopOptions.replOptions.ammoniteVersion,
            artifacts.userDependencies,
            artifacts.extraClassPath,
            artifacts.extraSourceJars,
            logger,
            cache,
            directories
          )
        else
          ReplArtifacts.default(
            scalaParams,
            artifacts.userDependencies,
            artifacts.extraClassPath,
            logger,
            cache,
            options.finalRepositories
          )
      maybeReplArtifacts match {
        case Left(FetchingDependenciesError(e: ResolutionError.CantDownloadModule, positions))
            if shouldUseAmmonite && e.module.name.value == s"ammonite_${scalaParams.scalaVersion}" =>
          Left(CantDownloadAmmoniteError(e.version, scalaParams.scalaVersion, e, positions))
        case either @ _ => either
      }
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

    val additionalArgs =
      if (shouldUseAmmonite)
        options.notForBloopOptions.replOptions.ammoniteArgs
      else
        options.scalaOptions.scalacOptions.toSeq.map(_.value.value)

    val replArgs = additionalArgs ++ programArgs

    if (dryRun)
      logger.message("Dry run, not running REPL.")
    else
      Runner.runJvm(
        options.javaHome().value.javaCommand,
        replArtifacts.replJavaOpts ++ options.javaOptions.javaOpts.toSeq.map(_.value.value),
        classDir.toSeq ++ replArtifacts.replClassPath,
        replArtifacts.replMainClass,
        if (Properties.isWin)
          replArgs.map { a =>
            if (a.contains(" ")) "\"" + a.replace("\"", "\\\"") + "\""
            else a
          }
        else
          replArgs,
        logger,
        allowExecve = allowExit
      ).waitFor()
  }
}
