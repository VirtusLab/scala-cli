package scala.cli.commands

import caseapp._

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.{Constants, Runner}
import scala.build.{Build, Logger}

object Test extends ScalaCommand[TestOptions] {
  override def group                               = "Main"
  override def sharedOptions(options: TestOptions) = Some(options.shared)
  def run(options: TestOptions, args: RemainingArgs): Unit = {

    val inputs = options.shared.inputsOrExit(args)

    val initialBuildOptions = options.buildOptions
    val bloopRifleConfig    = options.shared.bloopRifleConfig()
    val logger              = options.shared.logger

    def maybeTest(build: Build, allowExit: Boolean): Unit =
      build match {
        case s: Build.Successful =>
          testOnce(
            inputs.workspace,
            inputs.projectName,
            s,
            args.unparsed,
            logger,
            allowExecve = allowExit,
            exitOnError = allowExit
          )
        case _: Build.Failed =>
          System.err.println("Compilation failed")
          if (allowExit)
            sys.exit(1)
      }

    if (options.watch.watch) {
      val watcher = Build.watch(
        inputs,
        initialBuildOptions,
        bloopRifleConfig,
        logger,
        crossBuilds = false,
        postAction = () => WatchUtil.printWatchMessage()
      ) { res =>
        for (builds <- res.orReport(logger))
          maybeTest(builds.main, allowExit = false)
      }
      try WatchUtil.waitForCtrlC()
      finally watcher.dispose()
    }
    else {
      val builds =
        Build.build(inputs, initialBuildOptions, bloopRifleConfig, logger, crossBuilds = false)
          .orExit(logger)
      maybeTest(builds.main, allowExit = true)
    }
  }

  private def testOnce(
    root: os.Path,
    projectName: String,
    build: Build.Successful,
    args: Seq[String],
    logger: Logger,
    allowExecve: Boolean,
    exitOnError: Boolean
  ): Either[BuildException, Unit] = either {

    val testFrameworkOpt = build.options.testOptions.frameworkOpt

    val retCode: Int =
      if (build.options.scalaJsOptions.enable) {
        val linkerConfig = build.options.scalaJsOptions.linkerConfig
        value {
          Run.withLinkedJs(build, None, addTestInitializer = true, linkerConfig) { js =>
            Runner.testJs(
              build.fullClassPath,
              js.toIO,
              args,
              testFrameworkOpt
            )
          }
        }
      }
      else if (build.options.scalaNativeOptions.enable)
        value {
          Run.withNativeLauncher(
            build,
            "scala.scalanative.testinterface.TestMain",
            build.options.scalaNativeOptions.config.getOrElse(???),
            build.options.scalaNativeOptions.nativeWorkDir(root, projectName),
            logger.scalaNativeLogger
          ) { launcher =>
            Runner.testNative(
              build.fullClassPath,
              launcher.toIO,
              testFrameworkOpt,
              args,
              logger.scalaNativeLogger
            )
          }
        }
      else {
        val extraArgs =
          testFrameworkOpt.map(fw => s"--test-framework=$fw").toSeq ++ Seq("--") ++ args

        Runner.runJvm(
          build.options.javaCommand(),
          build.options.javaOptions.javaOpts,
          build.fullClassPath.map(_.toFile),
          Constants.testRunnerMainClass,
          extraArgs,
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
  }
}
