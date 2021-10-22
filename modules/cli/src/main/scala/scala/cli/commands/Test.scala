package scala.cli.commands

import caseapp._

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.internal.{Constants, Runner}
import scala.build.options.{Platform, Scope}
import scala.build.{Build, Builds, CrossKey, Logger}

object Test extends ScalaCommand[TestOptions] {
  override def group                               = "Main"
  override def sharedOptions(options: TestOptions) = Some(options.shared)

  private def gray  = "\u001b[90m"
  private def reset = Console.RESET

  def run(options: TestOptions, args: RemainingArgs): Unit = {
    val inputs = options.shared.inputsOrExit(args)
    val logger = options.shared.logger
    SetupIde.runSafe(
      options.shared,
      inputs,
      logger,
      Some(name)
    )

    val initialBuildOptions = options.buildOptions
    val bloopRifleConfig    = options.shared.bloopRifleConfig()

    val cross = options.compileCross.cross.getOrElse(false)

    def maybeTest(builds: Builds, allowExit: Boolean): Unit = {
      val optionsKeys = builds.map.keys.toVector.map(_.optionsKey).distinct
      val builds0 = optionsKeys.map { optionsKey =>
        builds.map.get(CrossKey(optionsKey, Scope.Test))
          .orElse(builds.map.get(CrossKey(optionsKey, Scope.Main)))
          // Can this happen in practice now?
          .getOrElse(sys.error(s"Main build not found for $optionsKey"))
      }
      val buildsLen = builds0.length
      val printBeforeAfterMessages =
        buildsLen > 1 && options.shared.logging.verbosity >= 0
      val results =
        for ((s, idx) <- builds0.zipWithIndex) yield {
          if (printBeforeAfterMessages) {
            val optionsKey = s.crossKey.optionsKey
            System.err.println(
              s"${gray}Running tests for Scala ${optionsKey.scalaVersion}, ${optionsKey.platform.repr}$reset"
            )
            System.err.println()
          }
          val retCodeOrError = testOnce(
            inputs.workspace,
            inputs.projectName,
            s,
            options.requireTests,
            args.unparsed,
            logger,
            allowExecve = allowExit && buildsLen <= 1
          )
          if (printBeforeAfterMessages && idx < buildsLen - 1)
            System.err.println()
          retCodeOrError
        }

      val maybeRetCodes = results.sequence
        .left.map(CompositeBuildException(_))

      val retCodesOpt =
        if (allowExit)
          Some(maybeRetCodes.orExit(logger))
        else
          maybeRetCodes.orReport(logger)

      for (retCodes <- retCodesOpt if !retCodes.forall(_ == 0))
        if (allowExit)
          sys.exit(retCodes.find(_ != 0).getOrElse(1))
        else {
          val red      = Console.RED
          val lightRed = "\u001b[91m"
          val reset    = Console.RESET
          System.err.println(
            s"${red}Tests exited with return code $lightRed${retCodes.mkString(", ")}$red.$reset"
          )
        }
    }

    if (options.watch.watch) {
      val watcher = Build.watch(
        inputs,
        initialBuildOptions,
        bloopRifleConfig,
        logger,
        crossBuilds = cross,
        postAction = () => WatchUtil.printWatchMessage()
      ) { res =>
        for (builds <- res.orReport(logger))
          maybeTest(builds, allowExit = false)
      }
      try WatchUtil.waitForCtrlC()
      finally watcher.dispose()
    }
    else {
      val builds =
        Build.build(inputs, initialBuildOptions, bloopRifleConfig, logger, crossBuilds = cross)
          .orExit(logger)
      maybeTest(builds, allowExit = true)
    }
  }

  private def testOnce(
    root: os.Path,
    projectName: String,
    build: Build.Successful,
    requireTests: Boolean,
    args: Seq[String],
    logger: Logger,
    allowExecve: Boolean
  ): Either[BuildException, Int] = either {

    val testFrameworkOpt = build.options.testOptions.frameworkOpt

    build.options.platform match {
      case Platform.JS =>
        val linkerConfig = build.options.scalaJsOptions.linkerConfig
        value {
          Run.withLinkedJs(build, None, addTestInitializer = true, linkerConfig) { js =>
            Runner.testJs(
              build.fullClassPath,
              js.toIO,
              requireTests,
              args,
              testFrameworkOpt,
              logger
            )
          }
        }
      case Platform.Native =>
        value {
          Run.withNativeLauncher(
            build,
            "scala.scalanative.testinterface.TestMain",
            build.options.scalaNativeOptions.config,
            build.options.scalaNativeOptions.nativeWorkDir(root, projectName),
            logger.scalaNativeLogger
          ) { launcher =>
            Runner.testNative(
              build.fullClassPath,
              launcher.toIO,
              testFrameworkOpt,
              requireTests,
              args,
              logger
            )
          }
        }
      case Platform.JVM =>
        val extraArgs =
          (if (requireTests) Seq("--require-tests") else Nil) ++
            testFrameworkOpt.map(fw => s"--test-framework=$fw").toSeq ++
            Seq("--") ++ args

        Runner.runJvm(
          build.options.javaCommand().javaCommand,
          build.options.javaOptions.javaOpts,
          build.fullClassPath.map(_.toFile),
          Constants.testRunnerMainClass,
          extraArgs,
          logger,
          allowExecve = allowExecve
        )
    }
  }
}
