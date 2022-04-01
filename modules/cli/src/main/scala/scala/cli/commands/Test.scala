package scala.cli.commands

import caseapp._

import java.nio.file.Path

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.internal.{Constants, Runner}
import scala.build.options.{BuildOptions, JavaOpt, Platform, Scope}
import scala.build.testrunner.AsmTestRunner
import scala.build.{Build, BuildThreads, Builds, CrossKey, Logger, Positioned}
import scala.cli.CurrentParams
import scala.cli.commands.util.SharedOptionsUtil._

object Test extends ScalaCommand[TestOptions] {
  override def group                               = "Main"
  override def sharedOptions(options: TestOptions) = Some(options.shared)

  private def gray  = "\u001b[90m"
  private def reset = Console.RESET

  private def buildOptions(opts: TestOptions): BuildOptions = {
    import opts._
    val baseOptions = shared.buildOptions(enableJmh = false, jmhVersion = None)
    baseOptions.copy(
      javaOptions = baseOptions.javaOptions.copy(
        javaOpts =
          baseOptions.javaOptions.javaOpts ++
            sharedJava.allJavaOpts.map(JavaOpt(_)).map(Positioned.commandLine _)
      ),
      testOptions = baseOptions.testOptions.copy(
        frameworkOpt = testFramework.map(_.trim).filter(_.nonEmpty)
      ),
      internalDependencies = baseOptions.internalDependencies.copy(
        addTestRunnerDependencyOpt = Some(true)
      )
    )
  }

  def run(options: TestOptions, args: RemainingArgs): Unit = {
    maybePrintGroupHelp(options)
    CurrentParams.verbosity = options.shared.logging.verbosity
    val inputs = options.shared.inputsOrExit(args.remaining)
    CurrentParams.workspaceOpt = Some(inputs.workspace)
    val logger = options.shared.logger
    SetupIde.runSafe(
      options.shared,
      inputs,
      logger,
      Some(name)
    )
    if (CommandUtils.shouldCheckUpdate)
      Update.checkUpdateSafe(logger)

    val initialBuildOptions = buildOptions(options)
    val threads             = BuildThreads.create()

    val compilerMaker = options.shared.compilerMaker(threads)

    val cross = options.compileCross.cross.getOrElse(false)

    def maybeTest(builds: Builds, allowExit: Boolean): Unit = {
      val optionsKeys = builds.map.keys.toVector.map(_.optionsKey).distinct
      val builds0 = optionsKeys.flatMap { optionsKey =>
        builds.map.get(CrossKey(optionsKey, Scope.Test))
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

    if (options.watch.watchMode) {
      val watcher = Build.watch(
        inputs,
        initialBuildOptions,
        compilerMaker,
        None,
        logger,
        crossBuilds = cross,
        buildTests = true,
        partial = None,
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
        Build.build(
          inputs,
          initialBuildOptions,
          compilerMaker,
          None,
          logger,
          crossBuilds = cross,
          buildTests = true,
          partial = None
        )
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

    build.options.platform.value match {
      case Platform.JS =>
        val linkerConfig = build.options.scalaJsOptions.linkerConfig(logger)
        value {
          Run.withLinkedJs(
            build,
            None,
            addTestInitializer = true,
            linkerConfig,
            build.options.scalaJsOptions.fullOpt.getOrElse(false),
            build.options.scalaJsOptions.noOpt.getOrElse(false),
            logger
          ) { js =>
            Runner.testJs(
              build.fullClassPath,
              js.toIO,
              requireTests,
              args,
              testFrameworkOpt,
              logger,
              build.options.scalaJsOptions.dom.getOrElse(false)
            )
          }.flatMap(e => e)
        }
      case Platform.Native =>
        value {
          Run.withNativeLauncher(
            build,
            "scala.scalanative.testinterface.TestMain",
            build.options.scalaNativeOptions.nativeWorkDir(root, projectName),
            logger
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
        val classPath = build.fullClassPath

        val testFrameworkOpt0 = testFrameworkOpt.orElse {
          findTestFramework(classPath, logger)
        }

        val extraArgs =
          (if (requireTests) Seq("--require-tests") else Nil) ++
            build.options.internal.verbosity.map(v => s"--verbosity=$v") ++
            testFrameworkOpt0.map(fw => s"--test-framework=$fw").toSeq ++
            Seq("--") ++ args

        Runner.runJvm(
          build.options.javaHome().value.javaCommand,
          build.options.javaOptions.javaOpts.toSeq.map(_.value.value),
          classPath.map(_.toFile),
          Constants.testRunnerMainClass,
          extraArgs,
          logger,
          allowExecve = allowExecve
        ).waitFor()
    }
  }

  def findTestFramework(classPath: Seq[Path], logger: Logger): Option[String] = {
    val classPath0 = classPath.map(_.toString)

    // https://github.com/VirtusLab/scala-cli/issues/426
    if (
      classPath0.exists(_.contains("zio-test")) && !classPath0.exists(_.contains("zio-test-sbt"))
    ) {
      val parentInspector = new AsmTestRunner.ParentInspector(classPath)
      Runner.frameworkName(classPath, parentInspector) match {
        case Right(f) => Some(f)
        case Left(_) =>
          logger.message(
            "zio-test found in the class path, zio-test-sbt should be added to run zio tests with Scala CLI."
          )
          None
      }
    }
    else
      None
  }

}
