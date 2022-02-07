package scala.cli.commands

import caseapp._

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.Runner
import scala.build.options.{BuildOptions, JavaOpt}
import scala.build.{Artifacts, Build, Inputs, Logger, Os, ReplArtifacts}
import scala.cli.CurrentParams
import scala.util.Properties

object Repl extends ScalaCommand[ReplOptions] {
  override def group = "Main"
  override def names = List(
    List("console"),
    List("repl")
  )
  override def sharedOptions(options: ReplOptions) = Some(options.shared)
  def run(options: ReplOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.shared.logging.verbosity
    def default = Inputs.default().getOrElse {
      Inputs.empty(Os.pwd)
    }
    val inputs = options.shared.inputsOrExit(args, defaultInputs = () => Some(default))
    CurrentParams.workspaceOpt = Some(inputs.workspace)

    val initialBuildOptions = options.buildOptions
    val bloopRifleConfig    = options.shared.bloopRifleConfig()
    val logger              = options.shared.logger

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
        artifacts,
        classDir,
        directories,
        logger,
        allowExit = allowExit,
        options.replDryRun
      )
      res match {
        case Left(ex) =>
          if (allowExit) logger.exit(ex)
          else logger.log(ex)
        case Right(()) =>
      }
    }

    val cross = options.compileCross.cross.getOrElse(false)

    if (inputs.isEmpty) {
      val artifacts = initialBuildOptions.artifacts(logger).orExit(logger)
      doRunRepl(initialBuildOptions, artifacts, None, allowExit = !options.watch.watch)
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
        Build.build(inputs, initialBuildOptions, bloopRifleConfig, logger, crossBuilds = cross)
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
    artifacts: Artifacts,
    classDir: Option[os.Path],
    directories: scala.build.Directories,
    logger: Logger,
    allowExit: Boolean,
    dryRun: Boolean
  ): Either[BuildException, Unit] = either {

    val replArtifacts = value {
      if (options.replOptions.useAmmonite)
        ReplArtifacts.ammonite(
          artifacts.params,
          options.replOptions.ammoniteVersion,
          artifacts.dependencies,
          artifacts.extraClassPath,
          artifacts.extraSourceJars,
          logger,
          directories
        )
      else
        ReplArtifacts.default(
          artifacts.params,
          artifacts.dependencies,
          artifacts.extraClassPath,
          logger,
          options.finalRepositories
        )
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
    if (rootClasses.nonEmpty && options.replOptions.useAmmoniteOpt.exists(_ == true))
      logger.message(
        s"Warning: found classes defined in the root package (${rootClasses.mkString(", ")})." +
          " These will not be accessible from the REPL."
      )

    if (dryRun)
      logger.message("Dry run, not running REPL.")
    else
      Runner.runJvm(
        options.javaHome().value.javaCommand,
        replArtifacts.replJavaOpts ++ JavaOpt.toStringSeq(options.javaOptions.javaOpts.values),
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
