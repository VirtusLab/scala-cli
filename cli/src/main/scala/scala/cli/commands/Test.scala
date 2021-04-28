package scala.cli.commands

import caseapp.core.app.CaseApp
import caseapp.core.RemainingArgs

import scala.cli.{Build, Inputs, Runner}
import scala.cli.internal.Constants

object Test extends CaseApp[TestOptions] {
  def run(options: TestOptions, args: RemainingArgs): Unit = {

    val inputs = Inputs(args.all, os.pwd) match {
      case Left(message) =>
        System.err.println(message)
        sys.exit(1)
      case Right(i) => i
    }

    val buildOptions = options.shared.buildOptions.copy(
      addTestRunnerDependencyOpt = Some(true)
    )
    if (options.shared.watch) {
      val watcher = Build.watch(inputs, buildOptions, options.shared.logger, os.pwd, postAction = () => WatchUtil.printWatchMessage()) { build =>
        testOnce(options.shared, inputs.workspace, inputs.projectName, build, allowExecve = false, exitOnError = false)
      }
      try WatchUtil.waitForCtrlC()
      finally watcher.dispose()
    } else {
      val build = Build.build(inputs, buildOptions, options.shared.logger, os.pwd)
      testOnce(options.shared, inputs.workspace, inputs.projectName, build, allowExecve = true, exitOnError = true)
    }
  }

  private def testOnce(
    options: SharedOptions,
    root: os.Path,
    projectName: String,
    build: Build,
    allowExecve: Boolean,
    exitOnError: Boolean
  ): Unit = {

    val retCode =
      if (options.js)
        Run.withLinkedJs(build, None, addTestInitializer = true) { js =>
          Runner.testJs(
            build.fullClassPath,
            js.toFile
          )
        }
      else if (options.native)
        Run.withNativeLauncher(
          build,
          "scala.scalanative.testinterface.TestMain",
          options.scalaNativeOptionsIKnowWhatImDoing,
          options.nativeWorkDir(root, projectName),
          options.scalaNativeLogger
        ) { launcher =>
          Runner.testNative(
            build.fullClassPath,
            launcher.toFile,
            options.logger,
            options.scalaNativeLogger
          )
        }
      else
        Runner.run(
          build.artifacts.javaHome.toIO,
          build.fullClassPath.map(_.toFile),
          Constants.testRunnerMainClass,
          Nil,
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
  }
}
