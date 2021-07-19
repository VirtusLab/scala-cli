package scala.cli.commands

import java.nio.file.Path

import caseapp._
import scala.build.{Build, Inputs, Logger, Os}
import scala.build.internal.{Constants, Runner}
import scala.build.options.BuildOptions
import scala.scalanative.{build => sn}

import org.scalajs.linker.interface.StandardConfig

object Run extends ScalaCommand[RunOptions] {
  override def group = "Main"

  override def sharedOptions(options: RunOptions) = Some(options.shared)

  def run(options: RunOptions, args: RemainingArgs): Unit =
    run(options, args, Some(Inputs.default()))

  def run(options: RunOptions, args: RemainingArgs, defaultInputs: Option[Inputs]): Unit = {

    val inputs = options.shared.inputsOrExit(args, defaultInputs)

    val buildOptions = options.buildOptions
    val bloopRifleConfig = options.shared.bloopRifleConfig()

    def maybeRun(build: Build.Successful, allowTerminate: Boolean): Unit =
      maybeRunOnce(
        options,
        inputs.workspace,
        inputs.projectName,
        build,
        args.unparsed,
        allowExecve = allowTerminate,
        exitOnError = allowTerminate,
        jvmRunner = build.options.addRunnerDependency
      )

    if (options.watch.watch) {
      val watcher = Build.watch(inputs, buildOptions, bloopRifleConfig, options.shared.logger, postAction = () => WatchUtil.printWatchMessage()) {
        case s: Build.Successful =>
          maybeRun(s, allowTerminate = false)
        case f: Build.Failed =>
          System.err.println("Compilation failed")
      }
      try WatchUtil.waitForCtrlC()
      finally watcher.dispose()
    } else {
      val build = Build.build(inputs, buildOptions, bloopRifleConfig, options.shared.logger)
      build match {
        case s: Build.Successful =>
          maybeRun(s, allowTerminate = true)
        case f: Build.Failed =>
          System.err.println("Compilation failed")
          sys.exit(1)
      }
    }
  }

  def maybeRunOnce(
    options: RunOptions,
    root: os.Path,
    projectName: String,
    build: Build.Successful,
    args: Seq[String],
    allowExecve: Boolean,
    exitOnError: Boolean,
    jvmRunner: Boolean
  ): Unit = {

    val mainClassOpt = options.mainClass.filter(_.nonEmpty) // trim it too?
      .orElse(if (build.options.jmhOptions.runJmh.contains(false)) Some("org.openjdk.jmh.Main") else None)
      .orElse(build.retainedMainClassOpt(warnIfSeveral = true))

    for (mainClass <- mainClassOpt) {
      val (finalMainClass, finalArgs) =
        if (jvmRunner) (Constants.runnerMainClass, mainClass +: args)
        else (mainClass, args)
      runOnce(
        options,
        root,
        projectName,
        build,
        finalMainClass,
        finalArgs,
        allowExecve,
        exitOnError
      )
    }
  }

  def runOnce(
    options: RunOptions,
    root: os.Path,
    projectName: String,
    build: Build.Successful,
    mainClass: String,
    args: Seq[String],
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
            options.shared.logger,
            allowExecve = allowExecve
          )
        }
      } else if (build.options.scalaNativeOptions.enable)
        withNativeLauncher(
          build,
          mainClass,
          build.options.scalaNativeOptions.config.getOrElse(???),
          options.shared.nativeWorkDir(root, projectName),
          options.shared.logger.scalaNativeLogger
        ) { launcher =>
          Runner.runNative(
            launcher.toIO,
            args,
            options.shared.logger,
            allowExecve = allowExecve
          )
        }
      else
        Runner.run(
          build.options.javaCommand(),
          build.options.javaOptions.javaOpts,
          build.fullClassPath.map(_.toFile),
          mainClass,
          args,
          options.shared.logger,
          allowExecve = allowExecve
        )

    if (retCode != 0) {
      if (exitOnError)
        sys.exit(retCode)
      else {
        val red = Console.RED
        val lightRed = "\u001b[91m"
        val reset = Console.RESET
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
    } finally {
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
    val dest = os.temp(prefix = "main", suffix = ".js")
    try {
      Package.buildNative(build, mainClass, dest, config, workDir, logger)
      f(dest)
    } finally {
      if (os.exists(dest))
        os.remove(dest)
    }
  }
}
