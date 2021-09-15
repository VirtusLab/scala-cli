package scala.cli.commands

import caseapp._
import org.scalajs.linker.interface.StandardConfig

import java.nio.file.Path

import scala.build.{Build, Inputs, Logger, Os}
import scala.build.internal.{Constants, Runner}
import scala.build.options.BuildOptions
import scala.scalanative.{build => sn}
import scala.util.Properties

object Run extends ScalaCommand[RunOptions] {
  override def group = "Main"

  override def sharedOptions(options: RunOptions) = Some(options.shared)

  def run(options: RunOptions, args: RemainingArgs): Unit =
    run(options, args, () => Inputs.default())

  def run(options: RunOptions, args: RemainingArgs, defaultInputs: () => Option[Inputs]): Unit = {

    val inputs = options.shared.inputsOrExit(args, defaultInputs = defaultInputs)

    val initialBuildOptions = options.buildOptions
    val bloopRifleConfig    = options.shared.bloopRifleConfig()
    val logger              = options.shared.logger

    def maybeRun(build: Build.Successful, allowTerminate: Boolean): Unit =
      maybeRunOnce(
        inputs.workspace,
        inputs.projectName,
        build,
        args.unparsed,
        logger,
        allowExecve = allowTerminate,
        exitOnError = allowTerminate,
        jvmRunner = build.options.addRunnerDependency.getOrElse(true)
      )

    val cross = options.compileCross.cross.getOrElse(false)

    if (options.watch.watch) {
      val watcher = Build.watch(
        inputs,
        initialBuildOptions,
        bloopRifleConfig,
        logger,
        crossBuilds = cross,
        postAction = () => WatchUtil.printWatchMessage()
      ) { res =>
        res.orReport(logger).map(_._1).foreach {
          case s: Build.Successful =>
            maybeRun(s, allowTerminate = false)
          case f: Build.Failed =>
            System.err.println("Compilation failed")
        }
      }
      try WatchUtil.waitForCtrlC()
      finally watcher.dispose()
    }
    else {
      val (build, _) =
        Build.build(inputs, initialBuildOptions, bloopRifleConfig, logger, crossBuilds = cross)
          .orExit(logger)
      build match {
        case s: Build.Successful =>
          maybeRun(s, allowTerminate = true)
        case f: Build.Failed =>
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
  ): Unit = {

    val mainClassOpt = build.options.mainClass.filter(_.nonEmpty) // trim it too?
      .orElse {
        if (build.options.jmhOptions.runJmh.contains(false)) Some("org.openjdk.jmh.Main")
        else None
      }
      .orElse(build.retainedMainClassOpt(warnIfSeveral = true))

    for (mainClass <- mainClassOpt) {
      val (finalMainClass, finalArgs) =
        if (jvmRunner) (Constants.runnerMainClass, mainClass +: args)
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

    val retCode =
      if (build.options.scalaJsOptions.enable) {
        val linkerConfig = build.options.scalaJsOptions.linkerConfig
        withLinkedJs(build, Some(mainClass), addTestInitializer = false, linkerConfig) { js =>
          Runner.runJs(
            js.toIO,
            args,
            logger,
            allowExecve = allowExecve
          )
        }
      }
      else if (build.options.scalaNativeOptions.enable)
        withNativeLauncher(
          build,
          mainClass,
          build.options.scalaNativeOptions.config.getOrElse(???),
          build.options.scalaNativeOptions.nativeWorkDir(root, projectName),
          logger.scalaNativeLogger
        ) { launcher =>
          Runner.runNative(
            launcher.toIO,
            args,
            logger,
            allowExecve = allowExecve
          )
        }
      else
        Runner.runJvm(
          build.options.javaCommand(),
          build.options.javaOptions.javaOpts,
          build.fullClassPath.map(_.toFile),
          mainClass,
          args,
          logger,
          allowExecve = allowExecve
        )

    if (retCode != 0) {
      if (exitOnError)
        sys.exit(retCode)
      else {
        val red      = Console.RED
        val lightRed = "\u001b[91m"
        val reset    = Console.RESET
        System.err.println(s"${red}Program exited with return code $lightRed$retCode$red.$reset")
      }
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
    finally {
      if (os.exists(dest))
        os.remove(dest)
    }
  }

  def withNativeLauncher[T](
    build: Build.Successful,
    mainClass: String,
    config: sn.NativeConfig,
    workDir: os.Path,
    logger: sn.Logger
  )(f: os.Path => T): T = {
    val dest = os.temp(prefix = "main", suffix = if (Properties.isWin) ".exe" else "")
    try {
      Package.buildNative(build, mainClass, dest, config, workDir, logger)
      f(dest)
    }
    finally {
      if (os.exists(dest))
        os.remove(dest)
    }
  }
}
