package scala.cli.commands

import caseapp._

import java.util.concurrent.CompletableFuture

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.{Constants, Runner, ScalaJsLinkerConfig}
import scala.build.options.{BuildOptions, JavaOpt, Platform}
import scala.build.{Build, BuildThreads, Inputs, Logger, Positioned}
import scala.cli.CurrentParams
import scala.cli.commands.util.MainClassOptionsUtil._
import scala.cli.commands.util.SharedOptionsUtil._
import scala.cli.internal.ProcUtil
import scala.util.Properties

object Run extends ScalaCommand[RunOptions] {
  override def group = "Main"

  override def sharedOptions(options: RunOptions): Option[SharedOptions] = Some(options.shared)

  def run(options: RunOptions, args: RemainingArgs): Unit = {
    maybePrintGroupHelp(options)
    run(
      options,
      args.remaining,
      args.unparsed,
      () => Inputs.default()
    )
  }

  def buildOptions(options: RunOptions): BuildOptions = {
    import options._
    val baseOptions = shared.buildOptions(
      enableJmh = benchmarking.jmh.contains(true),
      jmhVersion = benchmarking.jmhVersion
    )
    baseOptions.copy(
      mainClass = mainClass.mainClass,
      javaOptions = baseOptions.javaOptions.copy(
        javaOpts =
          baseOptions.javaOptions.javaOpts ++
            sharedJava.allJavaOpts.map(JavaOpt(_)).map(Positioned.commandLine)
      )
    )
  }

  def run(
    options: RunOptions,
    inputArgs: Seq[String],
    programArgs: Seq[String],
    defaultInputs: () => Option[Inputs]
  ): Unit = {
    CurrentParams.verbosity = options.shared.logging.verbosity
    val initialBuildOptions = buildOptions(options)
    maybePrintSimpleScalacOutput(options, initialBuildOptions)

    val inputs = options.shared.inputsOrExit(inputArgs, defaultInputs = defaultInputs)
    CurrentParams.workspaceOpt = Some(inputs.workspace)
    val logger  = options.shared.logger
    val threads = BuildThreads.create()

    val compilerMaker = options.shared.compilerMaker(threads)

    def maybeRun(
      build: Build.Successful,
      allowTerminate: Boolean
    ): Either[BuildException, (Process, CompletableFuture[_])] = either {
      val potentialMainClasses = build.foundMainClasses()
      value(options.mainClass.maybePrintMainClasses(potentialMainClasses))
      val process = value(maybeRunOnce(
        build,
        programArgs,
        logger,
        allowExecve = allowTerminate,
        jvmRunner = build.artifacts.hasJvmRunner,
        potentialMainClasses
      ))

      val onExitProcess = process.onExit().thenApply { p1 =>
        val retCode = p1.exitValue()
        if (retCode != 0)
          if (allowTerminate)
            sys.exit(retCode)
          else {
            val red      = Console.RED
            val lightRed = "\u001b[91m"
            val reset    = Console.RESET
            System.err.println(
              s"${red}Program exited with return code $lightRed$retCode$red.$reset"
            )
          }
      }

      (process, onExitProcess)
    }

    val cross = options.compileCross.cross.getOrElse(false)
    SetupIde.runSafe(
      options.shared,
      inputs,
      logger,
      Some(name),
      inputArgs
    )
    if (CommandUtils.shouldCheckUpdate)
      Update.checkUpdateSafe(logger)

    if (options.watch.watchMode) {
      var processOpt = Option.empty[(Process, CompletableFuture[_])]
      val watcher = Build.watch(
        inputs,
        initialBuildOptions,
        compilerMaker,
        None,
        logger,
        crossBuilds = cross,
        buildTests = false,
        partial = None,
        postAction = () => WatchUtil.printWatchMessage()
      ) { res =>
        for ((process, onExitProcess) <- processOpt) {
          onExitProcess.cancel(true)
          ProcUtil.interruptProcess(process, logger)
        }
        res.orReport(logger).map(_.main).foreach {
          case s: Build.Successful =>
            for ((proc, _) <- processOpt) // If the process doesn't exit, send SIGKILL
              if (proc.isAlive) ProcUtil.forceKillProcess(proc, logger)
            val maybeProcess = maybeRun(s, allowTerminate = false)
              .orReport(logger)
            if (options.watch.restart)
              processOpt = maybeProcess
            else
              for ((proc, onExit) <- maybeProcess)
                ProcUtil.waitForProcess(proc, onExit)
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
          compilerMaker,
          None,
          logger,
          crossBuilds = cross,
          buildTests = false,
          partial = None
        )
          .orExit(logger)
      builds.main match {
        case s: Build.Successful =>
          val (process, onExit) = maybeRun(s, allowTerminate = true)
            .orExit(logger)
          ProcUtil.waitForProcess(process, onExit)
        case _: Build.Failed =>
          System.err.println("Compilation failed")
          sys.exit(1)
      }
    }
  }

  private def maybeRunOnce(
    build: Build.Successful,
    args: Seq[String],
    logger: Logger,
    allowExecve: Boolean,
    jvmRunner: Boolean,
    potentialMainClasses: Seq[String]
  ): Either[BuildException, Process] = either {

    val mainClassOpt = build.options.mainClass.filter(_.nonEmpty) // trim it too?
      .orElse {
        if (build.options.jmhOptions.runJmh.contains(false)) Some("org.openjdk.jmh.Main")
        else None
      }
    val mainClass = mainClassOpt match {
      case Some(cls) => cls
      case None      => value(build.retainedMainClass(potentialMainClasses))
    }
    val verbosity = build.options.internal.verbosity.getOrElse(0).toString

    val (finalMainClass, finalArgs) =
      if (jvmRunner) (Constants.runnerMainClass, mainClass +: verbosity +: args)
      else (mainClass, args)
    val res = runOnce(
      build,
      finalMainClass,
      finalArgs,
      logger,
      allowExecve
    )
    value(res)
  }

  private def runOnce(
    build: Build.Successful,
    mainClass: String,
    args: Seq[String],
    logger: Logger,
    allowExecve: Boolean
  ): Either[BuildException, Process] = either {

    val process = build.options.platform.value match {
      case Platform.JS =>
        val linkerConfig = build.options.scalaJsOptions.linkerConfig(logger)
        val jsDest       = os.temp(prefix = "main", suffix = ".js")
        val res =
          Package.linkJs(
            build,
            jsDest,
            Some(mainClass),
            addTestInitializer = false,
            linkerConfig,
            build.options.scalaJsOptions.fullOpt,
            build.options.scalaJsOptions.noOpt.getOrElse(false),
            logger
          ).map { outputPath =>
            val process = Runner.runJs(
              outputPath.toIO,
              args,
              logger,
              allowExecve = allowExecve,
              jsDom = build.options.scalaJsOptions.dom.getOrElse(false),
              sourceMap = build.options.scalaJsOptions.emitSourceMaps
            )
            process.onExit().thenApply(_ => if (os.exists(jsDest)) os.remove(jsDest))
            process
          }
        value(res)
      case Platform.Native =>
        withNativeLauncher(
          build,
          mainClass,
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
          build.fullClassPath.map(_.toIO),
          mainClass,
          args,
          logger,
          allowExecve = allowExecve
        )
    }

    process
  }

  def withLinkedJs[T](
    build: Build.Successful,
    mainClassOpt: Option[String],
    addTestInitializer: Boolean,
    config: ScalaJsLinkerConfig,
    fullOpt: Boolean,
    noOpt: Boolean,
    logger: Logger
  )(f: os.Path => T): Either[BuildException, T] = {
    val dest = os.temp(prefix = "main", suffix = ".js")
    try Package.linkJs(
        build,
        dest,
        mainClassOpt,
        addTestInitializer,
        config,
        fullOpt,
        noOpt,
        logger
      ).map { outputPath =>
        f(outputPath)
      }
    finally if (os.exists(dest)) os.remove(dest)
  }

  def withNativeLauncher[T](
    build: Build.Successful,
    mainClass: String,
    logger: Logger
  )(f: os.Path => T): T = {
    val dest = build.inputs.nativeWorkDir / s"main${if (Properties.isWin) ".exe" else ""}"
    Package.buildNative(build, mainClass, dest, logger)
    f(dest)
  }
}
