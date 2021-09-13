package scala.cli.commands

import caseapp._
import scala.build.{Artifacts, Build, Inputs, Logger, Os, ReplArtifacts}
import scala.build.internal.Runner
import scala.build.options.BuildOptions
import scala.util.Properties

object Repl extends ScalaCommand[ReplOptions] {
  override def group = "Main"
  override def names = List(
    List("console"),
    List("repl")
  )
  override def sharedOptions(options: ReplOptions) = Some(options.shared)
  def run(options: ReplOptions, args: RemainingArgs): Unit = {

    def default = Inputs.default().getOrElse {
      Inputs.empty(Os.pwd)
    }
    val inputs = options.shared.inputsOrExit(args, defaultInputs = () => Some(default))

    val initialBuildOptions = options.buildOptions
    val bloopRifleConfig    = options.shared.bloopRifleConfig()
    val logger              = options.shared.logger

    val directories = options.shared.directories.directories

    val (build, _) =
      Build.build(inputs, initialBuildOptions, bloopRifleConfig, logger, crossBuilds = false)

    val successfulBuild = build.successfulOpt.getOrElse {
      System.err.println("Compilation failed")
      sys.exit(1)
    }

    def maybeRunRepl(
      buildOptions: BuildOptions,
      artifacts: Artifacts,
      classDir: Option[os.Path],
      allowExit: Boolean
    ): Unit =
      build match {
        case s: Build.Successful =>
          runRepl(
            buildOptions,
            artifacts,
            classDir,
            directories,
            logger,
            allowExit = allowExit,
            options.replDryRun
          )
        case f: Build.Failed =>
          System.err.println("Compilation failed")
          if (allowExit)
            sys.exit(1)
      }

    val cross = options.compileCross.cross.getOrElse(false)

    if (inputs.isEmpty) {
      val artifacts = initialBuildOptions.artifacts(logger)
      maybeRunRepl(initialBuildOptions, artifacts, None, allowExit = !options.watch.watch)
      if (options.watch.watch) {
        // nothing to watch, just wait for Ctrl+C
        WatchUtil.printWatchMessage()
        WatchUtil.waitForCtrlC()
      }
    }
    else if (options.watch.watch) {
      val watcher = Build.watch(
        inputs,
        initialBuildOptions,
        bloopRifleConfig,
        logger,
        crossBuilds = cross,
        postAction = () => WatchUtil.printWatchMessage()
      ) { (build, _) =>
        maybeRunRepl(build.options, build.artifacts, build.outputOpt, allowExit = false)
      }
      try WatchUtil.waitForCtrlC()
      finally watcher.dispose()
    }
    else {
      val (build, _) =
        Build.build(inputs, initialBuildOptions, bloopRifleConfig, logger, crossBuilds = cross)
      maybeRunRepl(build.options, build.artifacts, build.outputOpt, allowExit = true)
    }
  }

  private def runRepl(
    options: BuildOptions,
    artifacts: Artifacts,
    classDir: Option[os.Path],
    directories: scala.build.Directories,
    logger: Logger,
    allowExit: Boolean,
    dryRun: Boolean
  ): Unit = {

    val replArtifacts =
      if (options.replOptions.useAmmonite)
        ReplArtifacts.ammonite(
          artifacts.params,
          options.replOptions.ammoniteVersion,
          artifacts.dependencies,
          artifacts.extraJars,
          artifacts.extraSourceJars,
          logger,
          directories
        )
      else
        ReplArtifacts.default(
          artifacts.params,
          artifacts.dependencies,
          artifacts.extraJars,
          logger,
          directories
        )

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
    if (rootClasses.nonEmpty)
      logger.message(
        s"Warning: found classes defined in the root package (${rootClasses.mkString(", ")})." +
          " These will not be accessible from the REPL."
      )

    if (dryRun)
      logger.message("Dry run, not running REPL.")
    else
      Runner.runJvm(
        options.javaCommand(),
        replArtifacts.replJavaOpts ++ options.javaOptions.javaOpts,
        classDir.map(_.toIO).toSeq ++ replArtifacts.replClassPath.map(_.toFile),
        replArtifacts.replMainClass,
        if (Properties.isWin)
          options.replOptions.ammoniteArgs.map { a =>
            if (a.contains(" ")) "\"" + a.replace("\"", "\\\"") + "\""
            else a
          }
        else
          options.replOptions.ammoniteArgs,
        logger,
        allowExecve = allowExit
      )
  }
}
