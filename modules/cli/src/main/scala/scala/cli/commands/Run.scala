package scala.cli.commands

import caseapp._
import org.scalajs.linker.interface.StandardConfig

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.{Constants, Runner}
import scala.build.options.Platform
import scala.build.{Build, Inputs, Logger}
import scala.cli.CurrentParams
import scala.util.Properties

object Run extends ScalaCommand[RunOptions] {
  override def group = "Main"

  override def sharedOptions(options: RunOptions) = Some(options.shared)

  def run(options: RunOptions, args: RemainingArgs): Unit = {
    maybePrintGroupHelp(options)
    run(
      options,
      args.remaining,
      args.unparsed,
      () => Inputs.default()
    )
  }

  def run(
    options: RunOptions,
    inputArgs: Seq[String],
    programArgs: Seq[String],
    defaultInputs: () => Option[Inputs]
  ): Unit = {
    CurrentParams.verbosity = options.shared.logging.verbosity
    val inputs = options.shared.inputsOrExit(inputArgs, defaultInputs = defaultInputs)
    CurrentParams.workspaceOpt = Some(inputs.workspace)

    val initialBuildOptions = options.buildOptions
    val bloopRifleConfig    = options.shared.bloopRifleConfig()
    val logger              = options.shared.logger

    def maybeRun(build: Build.Successful, allowTerminate: Boolean): Either[BuildException, Unit] =
      maybeRunOnce(
        inputs.workspace,
        inputs.projectName,
        build,
        programArgs,
        logger,
        allowExecve = allowTerminate,
        exitOnError = allowTerminate,
        jvmRunner = build.options.addRunnerDependency.getOrElse(true)
      )

    val cross = options.compileCross.cross.getOrElse(false)
    SetupIde.runSafe(
      options.shared,
      inputs,
      logger,
      Some(name)
    )
    if (CommandUtils.shouldCheckUpdate)
      Update.checkUpdateSafe(logger)

    if (options.watch.watch) {
      val watcher = Build.watch(
        inputs,
        initialBuildOptions,
        bloopRifleConfig,
        logger,
        crossBuilds = cross,
        postAction = () => WatchUtil.printWatchMessage(),
        buildTests = false
      ) { res =>
        res.orReport(logger).map(_.main).foreach {
          case s: Build.Successful =>
            maybeRun(s, allowTerminate = false)
              .orReport(logger)
          case _: Build.Failed =>
            System.err.println("Compilation failed")
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
          bloopRifleConfig,
          logger,
          crossBuilds = cross,
          buildTests = false
        )
          .orExit(logger)
      builds.main match {
        case s: Build.Successful =>
          maybeRun(s, allowTerminate = true)
            .orExit(logger)
        case _: Build.Failed =>
          System.err.println("Compilation failed")
          sys.exit(1)
      }
    }
  }

  private def maybeRunOnce(
    root: os.Path,
    projectName: String,
    build: Build.Successful,
    args: Seq[String],
    logger: Logger,
    allowExecve: Boolean,
    exitOnError: Boolean,
    jvmRunner: Boolean
  ): Either[BuildException, Unit] = either {

    val mainClassOpt = build.options.mainClass.filter(_.nonEmpty) // trim it too?
      .orElse {
        if (build.options.jmhOptions.runJmh.contains(false)) Some("org.openjdk.jmh.Main")
        else None
      }
    val mainClass = mainClassOpt match {
      case Some(cls) => cls
      case None      => value(build.retainedMainClass)
    }
    val verbosity = build.options.internal.verbosity.getOrElse(0).toString

    val (finalMainClass, finalArgs) =
      if (jvmRunner) (Constants.runnerMainClass, mainClass +: verbosity +: args)
      else (mainClass, args)
    runOnce(
      root,
      projectName,
      build,
      finalMainClass,
      finalArgs,
      logger,
      allowExecve,
      exitOnError
    )
  }

  private def runOnce(
    root: os.Path,
    projectName: String,
    build: Build.Successful,
    mainClass: String,
    args: Seq[String],
    logger: Logger,
    allowExecve: Boolean,
    exitOnError: Boolean
  ): Boolean = {

    val retCode = build.options.platform.value match {
      case Platform.JS =>
        val linkerConfig = build.options.scalaJsOptions.linkerConfig(logger)
        withLinkedJs(build, Some(mainClass), addTestInitializer = false, linkerConfig) { js =>
          Runner.runJs(
            js.toIO,
            args,
            logger,
            allowExecve = allowExecve
          )
        }
      case Platform.Native =>
        withNativeLauncher(
          build,
          mainClass,
          build.options.scalaNativeOptions.nativeWorkDir(root, projectName),
          logger
        ) { launcher =>
          Runner.runNative(
            launcher.toIO,
            args,
            logger,
            allowExecve = allowExecve
          )
        }
      case Platform.JVM =>
        Runner.runJvm(
          build.options.javaHome().value.javaCommand,
          build.options.javaOptions.javaOpts.toSeq.map(_.value.value),
          build.fullClassPath.map(_.toFile),
          mainClass,
          args,
          logger,
          allowExecve = allowExecve
        )
    }

    if (retCode != 0)
      if (exitOnError)
        sys.exit(retCode)
      else {
        val red      = Console.RED
        val lightRed = "\u001b[91m"
        val reset    = Console.RESET
        System.err.println(s"${red}Program exited with return code $lightRed$retCode$red.$reset")
      }

    retCode == 0
  }

  def withLinkedJs[T](
    build: Build.Successful,
    mainClassOpt: Option[String],
    addTestInitializer: Boolean,
    config: StandardConfig
  )(f: os.Path => T): T = {
    val dest = os.temp(prefix = "main", suffix = ".js")
    try {
      Package.linkJs(build, dest, mainClassOpt, addTestInitializer, config)
      f(dest)
    }
    finally if (os.exists(dest)) os.remove(dest)
  }

  def withNativeLauncher[T](
    build: Build.Successful,
    mainClass: String,
    workDir: os.Path,
    logger: Logger
  )(f: os.Path => T): T = {
    val dest = workDir / s"main${if (Properties.isWin) ".exe" else ""}"
    Package.buildNative(build, mainClass, dest, workDir, logger)
    f(dest)
  }
}
