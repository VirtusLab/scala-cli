package scala.cli.commands

import caseapp.core.app.CaseApp
import caseapp.core.RemainingArgs
import scala.cli.{Build, Inputs, Runner}
import scala.cli.internal.Constants
import scala.scalanative.{build => sn}

import java.nio.file.{Files, Path}

object Run extends CaseApp[RunOptions] {
  def run(options: RunOptions, args: RemainingArgs): Unit = {

    val inputs = Inputs(args.remaining, os.pwd) match {
      case Left(message) =>
        System.err.println(message)
        sys.exit(1)
      case Right(i) => i
    }

    def maybeRun(build: Build, allowTerminate: Boolean): Unit =
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

    if (options.shared.watch) {
      val watcher = Build.watch(inputs, options.shared.buildOptions, options.shared.logger, os.pwd, postAction = () => WatchUtil.printWatchMessage()) { build =>
        maybeRun(build, allowTerminate = false)
      }
      try WatchUtil.waitForCtrlC()
      finally watcher.dispose()
    } else {
      val build = Build.build(inputs, options.shared.buildOptions, options.shared.logger, os.pwd)
      maybeRun(build, allowTerminate = true)
    }
  }

  def maybeRunOnce(
    options: RunOptions,
    root: os.Path,
    projectName: String,
    build: Build,
    args: Seq[String],
    allowExecve: Boolean,
    exitOnError: Boolean,
    jvmRunner: Boolean
  ): Unit = {

    val mainClassOpt = options.retainedMainClass.filter(_.nonEmpty) // trim it too?
      .orElse(build.retainedMainClassOpt(warnIfSeveral = true))

    for (mainClass <- mainClassOpt) {
      val (finalMainClass, finalArgs) =
        if (jvmRunner) (Constants.runnerMainClass, mainClass +: args)
        else (mainClass, args)
      runOnce(
        options.shared,
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
    options: SharedOptions,
    root: os.Path,
    projectName: String,
    build: Build,
    mainClass: String,
    args: Seq[String],
    allowExecve: Boolean,
    exitOnError: Boolean
  ): Boolean = {

    val retCode =
      if (options.js)
        withLinkedJs(build, Some(mainClass), addTestInitializer = false) { js =>
          Runner.runJs(
            js.toIO,
            args,
            options.logger,
            allowExecve = allowExecve
          )
        }
      else if (options.native)
        withNativeLauncher(
          build,
          mainClass,
          options.scalaNativeOptionsIKnowWhatImDoing,
          options.nativeWorkDir(root, projectName),
          options.scalaNativeLogger
        ) { launcher =>
          Runner.runNative(
            launcher.toIO,
            args,
            options.logger,
            allowExecve = allowExecve
          )
        }
      else
        Runner.run(
          build.artifacts.javaHome.toIO,
          build.fullClassPath.map(_.toFile),
          mainClass,
          args,
          options.logger,
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
    build: Build,
    mainClassOpt: Option[String],
    addTestInitializer: Boolean
  )(f: os.Path => T): T = {
    val dest = os.temp(prefix = "main", suffix = ".js")
    try {
      Package.linkJs(build, dest, mainClassOpt, addTestInitializer)
      f(dest)
    } finally {
      if (os.exists(dest))
        os.remove(dest)
    }
  }

  def withNativeLauncher[T](
    build: Build,
    mainClass: String,
    options: Build.ScalaNativeOptions,
    workDir: os.Path,
    logger: sn.Logger
  )(f: os.Path => T): T = {
    val dest = os.temp(prefix = "main", suffix = ".js")
    try {
      Package.buildNative(build, mainClass, dest, options, workDir, logger)
      f(dest)
    } finally {
      if (os.exists(dest))
        os.remove(dest)
    }
  }

}
