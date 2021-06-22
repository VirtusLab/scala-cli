package scala.cli.commands

import caseapp._

import scala.build.{Build, Inputs, Os, Runner}
import scala.build.internal.Constants

object Test extends ScalaCommand[TestOptions] {
  override def group = "Main"
  override def sharedOptions(options: TestOptions) = Some(options.shared)
  def run(options: TestOptions, args: RemainingArgs): Unit = {

    val inputs = options.shared.inputsOrExit(args, defaultInputs = Some(Inputs.default()))

    val buildOptions = options.buildOptions.copy(
      internalDependencies = options.buildOptions.internalDependencies.copy(
        addTestRunnerDependencyOpt = Some(true)
      )
    )
    val bloopRifleConfig = options.shared.bloopRifleConfig()

    if (options.watch.watch) {
      val watcher = Build.watch(inputs, buildOptions, bloopRifleConfig, options.shared.logger, Os.pwd, postAction = () => WatchUtil.printWatchMessage()) {
        case s: Build.Successful =>
          testOnce(options, inputs.workspace, inputs.projectName, s, allowExecve = false, exitOnError = false)
        case f: Build.Failed =>
          System.err.println("Compilation failed")
      }
      try WatchUtil.waitForCtrlC()
      finally watcher.dispose()
    } else {
      val build = Build.build(inputs, buildOptions, bloopRifleConfig, options.shared.logger, Os.pwd)
      build match {
        case s: Build.Successful =>
          testOnce(options, inputs.workspace, inputs.projectName, s, allowExecve = true, exitOnError = true)
        case f: Build.Failed =>
          System.err.println("Compilation failed")
          sys.exit(1)
      }
    }
  }

  private def testOnce(
    options: TestOptions,
    root: os.Path,
    projectName: String,
    build: Build.Successful,
    allowExecve: Boolean,
    exitOnError: Boolean
  ): Unit = {

    val retCode =
      if (options.shared.js.js) {
        val linkerConfig = build.options.scalaJsOptions.linkerConfig
        Run.withLinkedJs(build, None, addTestInitializer = true, linkerConfig) { js =>
          Runner.testJs(
            build.fullClassPath,
            js.toIO
          )
        }
      } else if (options.shared.native.native)
        Run.withNativeLauncher(
          build,
          "scala.scalanative.testinterface.TestMain",
          build.options.scalaNativeOptions.config.getOrElse(???),
          options.shared.nativeWorkDir(root, projectName),
          options.shared.scalaNativeLogger
        ) { launcher =>
          Runner.testNative(
            build.fullClassPath,
            launcher.toIO,
            options.shared.logger,
            options.shared.scalaNativeLogger
          )
        }
      else
        Runner.run(
          build.options.javaCommand(),
          options.sharedJava.allJavaOpts,
          build.fullClassPath.map(_.toFile),
          Constants.testRunnerMainClass,
          Nil,
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
  }
}
