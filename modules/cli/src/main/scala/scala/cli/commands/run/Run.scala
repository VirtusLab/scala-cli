package scala.cli.commands.run

import ai.kien.python.Python
import caseapp.*

import java.io.File
import java.util.Locale
import java.util.concurrent.CompletableFuture

import scala.build.EitherCps.{either, value}
import scala.build.*
import scala.build.errors.BuildException
import scala.build.input.{Inputs, ScalaCliInvokeData}
import scala.build.internal.{Constants, Runner, ScalaJsLinkerConfig}
import scala.build.options.{BuildOptions, JavaOpt, Platform, ScalacOpt}
import scala.cli.CurrentParams
import scala.cli.commands.package0.Package
import scala.cli.commands.publish.ConfigUtil.*
import scala.cli.commands.run.RunMode
import scala.cli.commands.setupide.SetupIde
import scala.cli.commands.shared.SharedOptions
import scala.cli.commands.update.Update
import scala.cli.commands.util.{BuildCommandHelpers, RunHadoop, RunSpark}
import scala.cli.commands.{CommandUtils, ScalaCommand, WatchUtil}
import scala.cli.config.{ConfigDb, Keys}
import scala.cli.internal.ProcUtil
import scala.util.{Properties, Try}

object Run extends ScalaCommand[RunOptions] with BuildCommandHelpers {
  override def group                                                     = "Main"
  override def scalaSpecificationLevel                                   = SpecificationLevel.MUST
  override def sharedOptions(options: RunOptions): Option[SharedOptions] = Some(options.shared)

  private def runMode(options: RunOptions): RunMode =
    if (
      options.sharedRun.standaloneSpark.getOrElse(false) &&
      !options.sharedRun.sparkSubmit.contains(false)
    )
      RunMode.StandaloneSparkSubmit(options.sharedRun.submitArgument)
    else if (options.sharedRun.sparkSubmit.getOrElse(false))
      RunMode.SparkSubmit(options.sharedRun.submitArgument)
    else if (options.sharedRun.hadoopJar)
      RunMode.HadoopJar
    else
      RunMode.Default

  private def scratchDirOpt(options: RunOptions): Option[os.Path] =
    options.sharedRun.scratchDir
      .filter(_.trim.nonEmpty)
      .map(os.Path(_, os.pwd))

  override def runCommand(options: RunOptions, args: RemainingArgs, logger: Logger): Unit =
    runCommand(
      options,
      args.remaining,
      args.unparsed,
      () => Inputs.default(),
      logger,
      invokeData
    )

  override def buildOptions(options: RunOptions): Some[BuildOptions] = Some {
    import options.*
    import options.sharedRun.*
    val logger = options.shared.logger
    val baseOptions = shared.buildOptions(
      enableJmh = benchmarking.jmh.contains(true),
      jmhVersion = benchmarking.jmhVersion
    ).orExit(logger)
    baseOptions.copy(
      mainClass = mainClass.mainClass,
      javaOptions = baseOptions.javaOptions.copy(
        javaOpts =
          baseOptions.javaOptions.javaOpts ++
            sharedJava.allJavaOpts.map(JavaOpt(_)).map(Positioned.commandLine),
        jvmIdOpt = baseOptions.javaOptions.jvmIdOpt.orElse {
          runMode(options) match {
            case _: RunMode.Spark | RunMode.HadoopJar =>
              Some(Positioned.none("8"))
            case RunMode.Default => None
          }
        }
      ),
      internal = baseOptions.internal.copy(
        keepResolution = baseOptions.internal.keepResolution || {
          runMode(options) match {
            case _: RunMode.Spark | RunMode.HadoopJar => true
            case RunMode.Default                      => false
          }
        }
      ),
      notForBloopOptions = baseOptions.notForBloopOptions.copy(
        runWithManifest = options.sharedRun.useManifest,
        addRunnerDependencyOpt = baseOptions.notForBloopOptions.addRunnerDependencyOpt.orElse {
          runMode(options) match {
            case _: RunMode.Spark | RunMode.HadoopJar =>
              Some(false)
            case RunMode.Default => None
          }
        }
      )
    )
  }

  def runCommand(
    options: RunOptions,
    inputArgs: Seq[String],
    programArgs: Seq[String],
    defaultInputs: () => Option[Inputs],
    logger: Logger,
    invokeData: ScalaCliInvokeData
  ): Unit = {
    val initialBuildOptions = buildOptionsOrExit(options)

    val inputs = options.shared.inputs(
      inputArgs,
      defaultInputs
    )(
      using invokeData
    ).orExit(logger)
    CurrentParams.workspaceOpt = Some(inputs.workspace)
    val threads = BuildThreads.create()

    val compilerMaker = options.shared.compilerMaker(threads).orExit(logger)

    def maybeRun(
      build: Build.Successful,
      allowTerminate: Boolean,
      runMode: RunMode,
      showCommand: Boolean,
      scratchDirOpt: Option[os.Path]
    ): Either[BuildException, Option[(Process, CompletableFuture[_])]] = either {
      val potentialMainClasses = build.foundMainClasses()
      if (options.sharedRun.mainClass.mainClassLs.contains(true))
        value {
          options.sharedRun.mainClass
            .maybePrintMainClasses(potentialMainClasses, shouldExit = allowTerminate)
            .map(_ => None)
        }
      else {
        val processOrCommand = value {
          maybeRunOnce(
            build,
            programArgs,
            logger,
            allowExecve = allowTerminate,
            jvmRunner = build.artifacts.hasJvmRunner,
            potentialMainClasses,
            runMode,
            showCommand,
            scratchDirOpt
          )
        }

        processOrCommand match {
          case Right((process, onExitOpt)) =>
            val onExitProcess = process.onExit().thenApply { p1 =>
              val retCode = p1.exitValue()
              onExitOpt.foreach(_())
              (retCode, allowTerminate) match {
                case (0, true) =>
                case (0, false) =>
                  val gray  = "\u001b[90m"
                  val reset = Console.RESET
                  System.err.println(s"${gray}Program exited with return code $retCode.$reset")
                case (_, true) =>
                  sys.exit(retCode)
                case (_, false) =>
                  val red      = Console.RED
                  val lightRed = "\u001b[91m"
                  val reset    = Console.RESET
                  System.err.println(
                    s"${red}Program exited with return code $lightRed$retCode$red.$reset"
                  )
              }
            }

            Some((process, onExitProcess))

          case Left(command) =>
            for (arg <- command)
              println(arg)
            None
        }
      }
    }

    val cross = options.sharedRun.compileCross.cross.getOrElse(false)
    SetupIde.runSafe(
      options.shared,
      inputs,
      logger,
      initialBuildOptions,
      Some(name),
      inputArgs
    )
    if (CommandUtils.shouldCheckUpdate)
      Update.checkUpdateSafe(logger)

    val configDb = options.shared.configDb.orExit(logger)
    val actionableDiagnostics =
      options.shared.logging.verbosityOptions.actions.orElse(
        configDb.get(Keys.actions).getOrElse(None)
      )

    if (options.sharedRun.watch.watchMode) {
      var processOpt      = Option.empty[(Process, CompletableFuture[_])]
      var shouldReadInput = false
      var mainThreadOpt   = Option.empty[Thread]
      val watcher = Build.watch(
        inputs,
        initialBuildOptions,
        compilerMaker,
        None,
        logger,
        crossBuilds = cross,
        buildTests = false,
        partial = None,
        actionableDiagnostics = actionableDiagnostics,
        postAction = () =>
          if (processOpt.exists(_._1.isAlive()))
            WatchUtil.printWatchWhileRunningMessage()
          else
            WatchUtil.printWatchMessage()
      ) { res =>
        for ((process, onExitProcess) <- processOpt) {
          onExitProcess.cancel(true)
          ProcUtil.interruptProcess(process, logger)
        }
        res.orReport(logger).map(_.main).foreach {
          case s: Build.Successful =>
            for ((proc, _) <- processOpt if proc.isAlive)
              // If the process doesn't exit, send SIGKILL
              ProcUtil.forceKillProcess(proc, logger)
            shouldReadInput = false
            mainThreadOpt.foreach(_.interrupt())
            val maybeProcess = maybeRun(
              s,
              allowTerminate = false,
              runMode = runMode(options),
              showCommand = options.sharedRun.command,
              scratchDirOpt = scratchDirOpt(options)
            )
              .orReport(logger)
              .flatten
              .map {
                case (proc, onExit) =>
                  if (options.sharedRun.watch.restart)
                    onExit.thenApply { _ =>
                      shouldReadInput = true
                      mainThreadOpt.foreach(_.interrupt())
                    }
                  (proc, onExit)
              }
            s.copyOutput(options.shared)
            if (options.sharedRun.watch.restart)
              processOpt = maybeProcess
            else {
              for ((proc, onExit) <- maybeProcess)
                ProcUtil.waitForProcess(proc, onExit)
              shouldReadInput = true
              mainThreadOpt.foreach(_.interrupt())
            }
          case _: Build.Failed =>
            System.err.println("Compilation failed")
        }
      }
      mainThreadOpt = Some(Thread.currentThread())

      try WatchUtil.waitForCtrlC(() => watcher.schedule(), () => shouldReadInput)
      finally {
        mainThreadOpt = None
        watcher.dispose()
      }
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
          partial = None,
          actionableDiagnostics = actionableDiagnostics
        )
          .orExit(logger)
      builds.main match {
        case s: Build.Successful =>
          s.copyOutput(options.shared)
          val res = maybeRun(
            s,
            allowTerminate = true,
            runMode = runMode(options),
            showCommand = options.sharedRun.command,
            scratchDirOpt = scratchDirOpt(options)
          )
            .orExit(logger)
          for ((process, onExit) <- res)
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
    potentialMainClasses: Seq[String],
    runMode: RunMode,
    showCommand: Boolean,
    scratchDirOpt: Option[os.Path]
  ): Either[BuildException, Either[Seq[String], (Process, Option[() => Unit])]] = either {

    val mainClassOpt = build.options.mainClass.filter(_.nonEmpty) // trim it too?
      .orElse {
        if (build.options.jmhOptions.runJmh.contains(false)) Some("org.openjdk.jmh.Main")
        else None
      }
    val mainClass = mainClassOpt match {
      case Some(cls) => cls
      case None      => value(build.retainedMainClass(logger, mainClasses = potentialMainClasses))
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
      allowExecve,
      runMode,
      showCommand,
      scratchDirOpt
    )
    value(res)
  }

  def pythonPathEnv(dirs: os.Path*): Map[String, String] = {
    val onlySafePaths = sys.env.exists {
      case (k, v) =>
        k.toLowerCase(Locale.ROOT) == "pythonsafepath" && v.nonEmpty
    }
    // Don't add unsafe directories to PYTHONPATH if PYTHONSAFEPATH is set,
    // see https://docs.python.org/3/using/cmdline.html#envvar-PYTHONSAFEPATH
    // and https://github.com/VirtusLab/scala-cli/pull/1616#issuecomment-1336017760
    // for more details.
    if (onlySafePaths) Map.empty[String, String]
    else {
      val (pythonPathEnvVarName, currentPythonPath) = sys.env
        .find(_._1.toLowerCase(Locale.ROOT) == "pythonpath")
        .getOrElse(("PYTHONPATH", ""))
      val updatedPythonPath = (currentPythonPath +: dirs.map(_.toString))
        .filter(_.nonEmpty)
        .mkString(File.pathSeparator)
      Map(pythonPathEnvVarName -> updatedPythonPath)
    }
  }

  private def runOnce(
    build: Build.Successful,
    mainClass: String,
    args: Seq[String],
    logger: Logger,
    allowExecve: Boolean,
    runMode: RunMode,
    showCommand: Boolean,
    scratchDirOpt: Option[os.Path]
  ): Either[BuildException, Either[Seq[String], (Process, Option[() => Unit])]] = either {

    build.options.platform.value match {
      case Platform.JS =>
        val esModule =
          build.options.scalaJsOptions.moduleKindStr.exists(m => m == "es" || m == "esmodule")

        val linkerConfig = build.options.scalaJsOptions.linkerConfig(logger)
        val jsDest = {
          val delete = scratchDirOpt.isEmpty
          scratchDirOpt.foreach(os.makeDir.all(_))
          os.temp(
            dir = scratchDirOpt.orNull,
            prefix = "main",
            suffix = if (esModule) ".mjs" else ".js",
            deleteOnExit = delete
          )
        }
        val res =
          Package.linkJs(
            build,
            jsDest,
            Some(mainClass),
            addTestInitializer = false,
            linkerConfig,
            build.options.scalaJsOptions.fullOpt,
            build.options.scalaJsOptions.noOpt.getOrElse(false),
            logger,
            scratchDirOpt
          ).map { outputPath =>
            val jsDom = build.options.scalaJsOptions.dom.getOrElse(false)
            if (showCommand)
              Left(Runner.jsCommand(outputPath.toIO, args, jsDom = jsDom))
            else {
              val process = Runner.runJs(
                outputPath.toIO,
                args,
                logger,
                allowExecve = allowExecve,
                jsDom = jsDom,
                sourceMap = build.options.scalaJsOptions.emitSourceMaps,
                esModule = esModule
              )
              process.onExit().thenApply(_ => if (os.exists(jsDest)) os.remove(jsDest))
              Right((process, None))
            }
          }
        value(res)
      case Platform.Native =>
        val setupPython = build.options.notForBloopOptions.doSetupPython.getOrElse(false)
        val (pythonExecutable, pythonLibraryPaths, pythonExtraEnv) =
          if (setupPython) {
            val (exec, libPaths) = value {
              val python = Python()
              val pythonPropertiesOrError = for {
                paths      <- python.nativeLibraryPaths
                executable <- python.executable
              } yield (Some(executable), paths)
              logger.debug(s"Python executable and native library paths: $pythonPropertiesOrError")
              pythonPropertiesOrError.orPythonDetectionError
            }
            // Putting the workspace in PYTHONPATH, see
            // https://github.com/VirtusLab/scala-cli/pull/1616#issuecomment-1333283174
            // for context.
            (exec, libPaths, pythonPathEnv(build.inputs.workspace))
          }
          else
            (None, Nil, Map())
        // seems conda doesn't add the lib directory to LD_LIBRARY_PATH (see conda/conda#308),
        // which prevents apps from finding libpython for example, so we update it manually here
        val libraryPathsEnv =
          if (pythonLibraryPaths.isEmpty) Map.empty
          else {
            val prependTo =
              if (Properties.isWin) "PATH"
              else if (Properties.isMac) "DYLD_LIBRARY_PATH"
              else "LD_LIBRARY_PATH"
            val currentOpt = Option(System.getenv(prependTo))
            val currentEntries = currentOpt
              .map(_.split(File.pathSeparator).toSet)
              .getOrElse(Set.empty)
            val additionalEntries = pythonLibraryPaths.filter(!currentEntries.contains(_))
            if (additionalEntries.isEmpty)
              Map.empty
            else {
              val newValue =
                (additionalEntries.iterator ++ currentOpt.iterator).mkString(File.pathSeparator)
              Map(prependTo -> newValue)
            }
          }
        val programNameEnv =
          pythonExecutable.fold(Map.empty)(py => Map("SCALAPY_PYTHON_PROGRAMNAME" -> py))
        val extraEnv = libraryPathsEnv ++ programNameEnv ++ pythonExtraEnv
        val maybeResult = withNativeLauncher(
          build,
          mainClass,
          logger
        ) { launcher =>
          if (showCommand)
            Left(
              extraEnv.toVector.sorted.map { case (k, v) => s"$k=$v" } ++
                Seq(launcher.toString) ++
                args
            )
          else {
            val proc = Runner.runNative(
              launcher.toIO,
              args,
              logger,
              allowExecve = allowExecve,
              extraEnv = extraEnv
            )
            Right((proc, None))
          }
        }
        value(maybeResult)
      case Platform.JVM =>
        runMode match {
          case RunMode.Default =>
            val baseJavaProps = build.options.javaOptions.javaOpts.toSeq.map(_.value.value)
            val setupPython   = build.options.notForBloopOptions.doSetupPython.getOrElse(false)
            val (pythonJavaProps, pythonExtraEnv) =
              if (setupPython) {
                val scalapyProps = value {
                  val python       = Python()
                  val propsOrError = python.scalapyProperties
                  logger.debug(s"Python Java properties: $propsOrError")
                  propsOrError.orPythonDetectionError
                }
                val props = scalapyProps.toVector.sorted.map {
                  case (k, v) => s"-D$k=$v"
                }
                // Putting the workspace in PYTHONPATH, see
                // https://github.com/VirtusLab/scala-cli/pull/1616#issuecomment-1333283174
                // for context.
                (props, pythonPathEnv(build.inputs.workspace))
              }
              else
                (Nil, Map.empty[String, String])
            val allJavaOpts = pythonJavaProps ++ baseJavaProps
            if (showCommand) {
              val command = Runner.jvmCommand(
                build.options.javaHome().value.javaCommand,
                allJavaOpts,
                build.fullClassPath,
                mainClass,
                args,
                extraEnv = pythonExtraEnv,
                useManifest = build.options.notForBloopOptions.runWithManifest,
                scratchDirOpt = scratchDirOpt
              )
              Left(command)
            }
            else {
              val proc = Runner.runJvm(
                build.options.javaHome().value.javaCommand,
                allJavaOpts,
                build.fullClassPath,
                mainClass,
                args,
                logger,
                allowExecve = allowExecve,
                extraEnv = pythonExtraEnv,
                useManifest = build.options.notForBloopOptions.runWithManifest,
                scratchDirOpt = scratchDirOpt
              )
              Right((proc, None))
            }
          case mode: RunMode.SparkSubmit =>
            value {
              RunSpark.run(
                build,
                mainClass,
                args,
                mode.submitArgs,
                logger,
                allowExecve,
                showCommand,
                scratchDirOpt
              )
            }
          case mode: RunMode.StandaloneSparkSubmit =>
            value {
              RunSpark.runStandalone(
                build,
                mainClass,
                args,
                mode.submitArgs,
                logger,
                allowExecve,
                showCommand,
                scratchDirOpt
              )
            }
          case RunMode.HadoopJar =>
            value {
              RunHadoop.run(
                build,
                mainClass,
                args,
                logger,
                allowExecve,
                showCommand,
                scratchDirOpt
              )
            }
        }
    }
  }

  def withLinkedJs[T](
    build: Build.Successful,
    mainClassOpt: Option[String],
    addTestInitializer: Boolean,
    config: ScalaJsLinkerConfig,
    fullOpt: Boolean,
    noOpt: Boolean,
    logger: Logger,
    esModule: Boolean
  )(f: os.Path => T): Either[BuildException, T] = {
    val dest = os.temp(prefix = "main", suffix = if (esModule) ".mjs" else ".js")
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
  )(f: os.Path => T): Either[BuildException, T] =
    Package.buildNative(build, mainClass, logger).map(f)

  final class PythonDetectionError(cause: Throwable) extends BuildException(
        s"Error detecting Python environment: ${cause.getMessage}",
        cause = cause
      )

  extension [T](t: Try[T])
    def orPythonDetectionError: Either[PythonDetectionError, T] =
      t.toEither.left.map(new PythonDetectionError(_))
}
