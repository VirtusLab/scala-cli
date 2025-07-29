package scala.cli.commands.run

import ai.kien.python.Python
import caseapp.*
import caseapp.core.help.HelpFormat

import java.io.File
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

import scala.build.*
import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.input.*
import scala.build.internal.{Constants, Runner, ScalaJsLinkerConfig}
import scala.build.internals.ConsoleUtils.ScalaCliConsole
import scala.build.internals.ConsoleUtils.ScalaCliConsole.warnPrefix
import scala.build.internals.EnvVar
import scala.build.options.{BuildOptions, JavaOpt, PackageType, Platform, Scope}
import scala.cli.CurrentParams
import scala.cli.commands.package0.Package
import scala.cli.commands.setupide.SetupIde
import scala.cli.commands.shared.{HelpCommandGroup, HelpGroup, SharedOptions}
import scala.cli.commands.update.Update
import scala.cli.commands.util.BuildCommandHelpers.*
import scala.cli.commands.util.{BuildCommandHelpers, RunHadoop, RunSpark}
import scala.cli.commands.{CommandUtils, ScalaCommand, SpecificationLevel, WatchUtil}
import scala.cli.config.Keys
import scala.cli.internal.ProcUtil
import scala.cli.packaging.Library.fullClassPathMaybeAsJar
import scala.cli.util.ArgHelpers.*
import scala.cli.util.ConfigDbUtils
import scala.util.{Properties, Try}

object Run extends ScalaCommand[RunOptions] with BuildCommandHelpers {
  override def group: String                               = HelpCommandGroup.Main.toString
  override def scalaSpecificationLevel: SpecificationLevel = SpecificationLevel.MUST

  val primaryHelpGroups: Seq[HelpGroup] = Seq(HelpGroup.Run, HelpGroup.Entrypoint, HelpGroup.Watch)
  override def helpFormat: HelpFormat   = super.helpFormat.withPrimaryGroups(primaryHelpGroups)
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
    val logger      = options.shared.logger
    val baseOptions = shared.buildOptions().orExit(logger)
    baseOptions.copy(
      mainClass = mainClass.mainClass,
      javaOptions = baseOptions.javaOptions.copy(
        javaOpts =
          baseOptions.javaOptions.javaOpts ++
            sharedJava.allJavaOpts.map(JavaOpt(_)).map(Positioned.commandLine),
        jvmIdOpt = baseOptions.javaOptions.jvmIdOpt.orElse {
          runMode(options) match {
            case _: RunMode.Spark | RunMode.HadoopJar =>
              val sparkOrHadoopDefaultJvm = "8"
              logger.message(
                s"Defaulting the JVM to  $sparkOrHadoopDefaultJvm for Spark/Hadoop runs."
              )
              Some(Positioned.none(sparkOrHadoopDefaultJvm))
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
              logger.debug(s"$warnPrefix Skipping the runner dependency when running Spark/Hadoop.")
              Some(false)
            case RunMode.Default => None
          }
        }
      )
    )
  }

  def runCommand(
    options0: RunOptions,
    inputArgs: Seq[String],
    programArgs: Seq[String],
    defaultInputs: () => Option[Inputs],
    logger: Logger,
    invokeData: ScalaCliInvokeData
  ): Unit = {
    val shouldDefaultServerFalse =
      inputArgs.isEmpty && options0.shared.compilationServer.server.isEmpty &&
      !options0.shared.hasSnippets
    val options = if (shouldDefaultServerFalse) {
      logger.debug("No inputs provided, skipping the build server.")
      options0.copy(shared =
        options0.shared.copy(compilationServer =
          options0.shared.compilationServer.copy(server = Some(false))
        )
      )
    }
    else options0
    val initialBuildOptions = {
      val buildOptions = buildOptionsOrExit(options)
      if (invokeData.subCommand == SubCommand.Shebang) {
        val suppressDepUpdateOptions = buildOptions.suppressWarningOptions.copy(
          suppressOutdatedDependencyWarning = Some(true)
        )

        buildOptions.copy(
          suppressWarningOptions = suppressDepUpdateOptions
        )
      }
      else buildOptions
    }

    val inputs = options.shared.inputs(
      inputArgs,
      defaultInputs
    )(
      using invokeData
    ).orExit(logger)
    CurrentParams.workspaceOpt = Some(inputs.workspace)
    val threads = BuildThreads.create()

    val compilerMaker = options.shared.compilerMaker(threads)

    def maybeRun(
      builds: Seq[Build.Successful],
      allowTerminate: Boolean,
      runMode: RunMode,
      showCommand: Boolean,
      scratchDirOpt: Option[os.Path]
    ): Either[BuildException, Seq[(Process, CompletableFuture[?])]] = either {
      val potentialMainClasses = builds.flatMap(_.foundMainClasses()).distinct
      if (options.sharedRun.mainClass.mainClassLs.contains(true))
        value {
          options.sharedRun.mainClass
            .maybePrintMainClasses(potentialMainClasses, shouldExit = allowTerminate)
            .map(_ => Seq.empty)
        }
      else {
        val processOrCommand: Either[Seq[Seq[String]], Seq[(Process, Option[() => Unit])]] = value {
          maybeRunOnce(
            builds = builds,
            args = programArgs,
            logger = logger,
            allowExecve = allowTerminate,
            jvmRunner = builds.exists(_.artifacts.hasJvmRunner),
            potentialMainClasses = potentialMainClasses,
            runMode = runMode,
            showCommand = showCommand,
            scratchDirOpt = scratchDirOpt,
            asJar = options.shared.asJar
          )
        }

        processOrCommand match {
          case Right(processes) =>
            processes.map { case (process, onExitOpt) =>
              val onExitProcess = process.onExit().thenApply { p1 =>
                val retCode = p1.exitValue()
                onExitOpt.foreach(_())
                (retCode, allowTerminate) match {
                  case (0, true)  =>
                  case (0, false) =>
                    val gray  = ScalaCliConsole.GRAY
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
              (process, onExitProcess)
            }
          case Left(commands) =>
            for {
              command <- commands
              arg     <- command
            } println(arg)
            Seq.empty
        }
      }
    }

    val cross = options.sharedRun.compileCross.cross.getOrElse(false)
    if cross then
      logger.log(
        "Cross builds enabled, preparing all builds for all Scala versions and platforms..."
      )
    SetupIde.runSafe(
      options = options.shared,
      inputs = inputs,
      logger = logger,
      buildOptions = initialBuildOptions,
      previousCommandName = Some(name),
      args = inputArgs
    )
    if CommandUtils.shouldCheckUpdate then Update.checkUpdateSafe(logger)

    val configDb              = ConfigDbUtils.configDb.orExit(logger)
    val actionableDiagnostics =
      options.shared.logging.verbosityOptions.actions.orElse(
        configDb.get(Keys.actions).getOrElse(None)
      )

    val shouldBuildTestScope = options.shared.scope.test.getOrElse(false)
    if shouldBuildTestScope then
      logger.log("Test scope enabled, including test scope inputs on the classpath...")
    if options.sharedRun.watch.watchMode then {

      /** A handle to the Runner processes, used to kill the process if it's still alive when a
        * change occured and restarts are allowed or to wait for it if restarts are not allowed
        */
      val processesRef = AtomicReference(Seq.empty[(Process, CompletableFuture[?])])

      /** shouldReadInput controls whether [[WatchUtil.waitForCtrlC]](that's keeping the main thread
        * alive) should try to read StdIn or just call wait()
        */
      val shouldReadInput = AtomicReference(false)

      /** A handle to the main thread to interrupt its operations when:
        *   - it's blocked on reading StdIn, and it's no longer required
        *   - it's waiting and should start reading StdIn
        */
      val mainThreadOpt = AtomicReference(Option.empty[Thread])

      val watcher = Build.watch(
        inputs = inputs,
        options = initialBuildOptions,
        compilerMaker = compilerMaker,
        docCompilerMakerOpt = None,
        logger = logger,
        crossBuilds = cross,
        buildTests = shouldBuildTestScope,
        partial = None,
        actionableDiagnostics = actionableDiagnostics,
        postAction = () =>
          if processesRef.get().exists(_._1.isAlive()) then
            WatchUtil.printWatchWhileRunningMessage()
          else WatchUtil.printWatchMessage()
      ) { res =>
        for ((process, onExitProcess) <- processesRef.get()) {
          onExitProcess.cancel(true)
          ProcUtil.interruptProcess(process, logger)
        }
        res.orReport(logger).map(_.builds).foreach {
          case b if b.forall(_.success) =>
            val successfulBuilds = b.collect { case s: Build.Successful => s }
            for ((proc, _) <- processesRef.get() if proc.isAlive)
              // If the process doesn't exit, send SIGKILL
              ProcUtil.forceKillProcess(proc, logger)
            shouldReadInput.set(false)
            mainThreadOpt.get().foreach(_.interrupt())
            val maybeProcesses = maybeRun(
              successfulBuilds,
              allowTerminate = false,
              runMode = runMode(options),
              showCommand = options.sharedRun.command,
              scratchDirOpt = scratchDirOpt(options)
            )
              .orReport(logger)
              .toSeq
              .flatten
              .map {
                case (proc, onExit) =>
                  if (options.sharedRun.watch.restart)
                    onExit.thenApply { _ =>
                      shouldReadInput.set(true)
                      mainThreadOpt.get().foreach(_.interrupt())
                    }
                  (proc, onExit)
              }
            successfulBuilds.foreach(_.copyOutput(options.shared))
            if options.sharedRun.watch.restart then processesRef.set(maybeProcesses)
            else {
              for ((proc, onExit) <- maybeProcesses)
                ProcUtil.waitForProcess(proc, onExit)
              shouldReadInput.set(true)
              mainThreadOpt.get().foreach(_.interrupt())
            }
          case b if b.exists(bb => !bb.success && !bb.cancelled) =>
            System.err.println("Compilation failed")
          case _ => ()
        }
      }
      mainThreadOpt.set(Some(Thread.currentThread()))

      try
        WatchUtil.waitForCtrlC(
          { () =>
            watcher.schedule()
            shouldReadInput.set(false)
          },
          () => shouldReadInput.get()
        )
      finally {
        mainThreadOpt.set(None)
        watcher.dispose()
      }
    }
    else
      Build.build(
        inputs = inputs,
        options = initialBuildOptions,
        compilerMaker = compilerMaker,
        docCompilerMakerOpt = None,
        logger = logger,
        crossBuilds = cross,
        buildTests = shouldBuildTestScope,
        partial = None,
        actionableDiagnostics = actionableDiagnostics
      )
        .orExit(logger)
        .all match {
        case b if b.forall(_.success) =>
          val successfulBuilds = b.collect { case s: Build.Successful => s }
          successfulBuilds.foreach(_.copyOutput(options.shared))
          val results = maybeRun(
            successfulBuilds,
            allowTerminate = true,
            runMode = runMode(options),
            showCommand = options.sharedRun.command,
            scratchDirOpt = scratchDirOpt(options)
          )
            .orExit(logger)
          for ((process, onExit) <- results)
            ProcUtil.waitForProcess(process, onExit)
        case b if b.exists(bb => !bb.success && !bb.cancelled) =>
          System.err.println("Compilation failed")
          sys.exit(1)
        case _ => ()
      }
  }

  private def maybeRunOnce(
    builds: Seq[Build.Successful],
    args: Seq[String],
    logger: Logger,
    allowExecve: Boolean,
    jvmRunner: Boolean,
    potentialMainClasses: Seq[String],
    runMode: RunMode,
    showCommand: Boolean,
    scratchDirOpt: Option[os.Path],
    asJar: Boolean
  ): Either[BuildException, Either[Seq[Seq[String]], Seq[(Process, Option[() => Unit])]]] = either {
    val mainClassOpt = builds.head.options.mainClass.filter(_.nonEmpty) // trim it too?
      .orElse {
        if builds.head.options.jmhOptions.enableJmh.contains(
            true
          ) && !builds.head.options.jmhOptions.canRunJmh
        then Some("org.openjdk.jmh.Main")
        else None
      }
    val mainClass: String = mainClassOpt match {
      case Some(cls) => cls
      case None      =>
        val retainedMainClassesByScope: Map[Scope, String] = value {
          builds
            .map { build =>
              build.retainedMainClass(logger, mainClasses = potentialMainClasses)
                .map(mainClass => build.scope -> mainClass)
            }
            .sequence
            .left
            .map(CompositeBuildException(_))
            .map(_.toMap)
        }
        if retainedMainClassesByScope.size == 1 then retainedMainClassesByScope.head._2
        else
          retainedMainClassesByScope
            .get(Scope.Main)
            .orElse(retainedMainClassesByScope.get(Scope.Test))
            .get
    }
    logger.debug(s"Retained main class: $mainClass")
    val verbosity = builds.head.options.internal.verbosity.getOrElse(0).toString

    val (finalMainClass, finalArgs) =
      if (jvmRunner) (Constants.runnerMainClass, mainClass +: verbosity +: args)
      else (mainClass, args)
    logger.debug(s"Final main class: $finalMainClass")
    val res = runOnce(
      builds,
      finalMainClass,
      finalArgs,
      logger,
      allowExecve,
      runMode,
      showCommand,
      scratchDirOpt,
      asJar
    )
    value(res)
  }

  def pythonPathEnv(dirs: os.Path*): Map[String, String] = {
    val onlySafePaths = sys.env.exists {
      case (k, v) => k.toLowerCase(Locale.ROOT) == "pythonsafepath" && v.nonEmpty
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
    allBuilds: Seq[Build.Successful],
    mainClass: String,
    args: Seq[String],
    logger: Logger,
    allowExecve: Boolean,
    runMode: RunMode,
    showCommand: Boolean,
    scratchDirOpt: Option[os.Path],
    asJar: Boolean
  ): Either[BuildException, Either[Seq[Seq[String]], Seq[(Process, Option[() => Unit])]]] = {
    val crossBuilds        = allBuilds.groupedByCrossParams.toSeq
    val shouldLogCrossInfo = crossBuilds.size > 1
    if shouldLogCrossInfo then
      logger.log(
        s"Running ${crossBuilds.size} cross builds, one for each Scala version and platform combination."
      )
    crossBuilds
      .map { (crossBuildParams, builds) =>
        if shouldLogCrossInfo then logger.debug(s"Running build for ${crossBuildParams.asString}")
        val build = builds.head
        either {
          build.options.platform.value match {
            case Platform.JS =>
              val esModule =
                build.options.scalaJsOptions.moduleKindStr.exists(m => m == "es" || m == "esmodule")

              val linkerConfig = builds.head.options.scalaJsOptions.linkerConfig(logger)
              val jsDest       = {
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
                  builds = builds,
                  dest = jsDest,
                  mainClassOpt = Some(mainClass),
                  addTestInitializer = false,
                  config = linkerConfig,
                  fullOpt = value(build.options.scalaJsOptions.fullOpt),
                  noOpt = build.options.scalaJsOptions.noOpt.getOrElse(false),
                  logger = logger,
                  scratchDirOpt = scratchDirOpt
                ).map { outputPath =>
                  val jsDom = build.options.scalaJsOptions.dom.getOrElse(false)
                  if (showCommand)
                    Left(Runner.jsCommand(outputPath.toIO, args, jsDom = jsDom))
                  else {
                    val process = value {
                      Runner.runJs(
                        outputPath.toIO,
                        args,
                        logger,
                        allowExecve = allowExecve,
                        jsDom = jsDom,
                        sourceMap = build.options.scalaJsOptions.emitSourceMaps,
                        esModule = esModule
                      )
                    }
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
                    val python                  = Python()
                    val pythonPropertiesOrError = for {
                      paths      <- python.nativeLibraryPaths
                      executable <- python.executable
                    } yield (Some(executable), paths)
                    logger.debug(
                      s"Python executable and native library paths: $pythonPropertiesOrError"
                    )
                    pythonPropertiesOrError.orPythonDetectionError
                  }
                  // Putting the workspace in PYTHONPATH, see
                  // https://github.com/VirtusLab/scala-cli/pull/1616#issuecomment-1333283174
                  // for context.
                  (exec, libPaths, pythonPathEnv(builds.head.inputs.workspace))
                }
                else
                  (None, Nil, Map())
              // seems conda doesn't add the lib directory to LD_LIBRARY_PATH (see conda/conda#308),
              // which prevents apps from finding libpython for example, so we update it manually here
              val libraryPathsEnv =
                if (pythonLibraryPaths.isEmpty) Map.empty
                else {
                  val prependTo =
                    if (Properties.isWin) EnvVar.Misc.path.name
                    else if (Properties.isMac) EnvVar.Misc.dyldLibraryPath.name
                    else EnvVar.Misc.ldLibraryPath.name
                  val currentOpt     = Option(System.getenv(prependTo))
                  val currentEntries = currentOpt
                    .map(_.split(File.pathSeparator).toSet)
                    .getOrElse(Set.empty)
                  val additionalEntries = pythonLibraryPaths.filter(!currentEntries.contains(_))
                  if (additionalEntries.isEmpty)
                    Map.empty
                  else {
                    val newValue =
                      (additionalEntries.iterator ++ currentOpt.iterator).mkString(
                        File.pathSeparator
                      )
                    Map(prependTo -> newValue)
                  }
                }
              val programNameEnv =
                pythonExecutable.fold(Map.empty)(py => Map("SCALAPY_PYTHON_PROGRAMNAME" -> py))
              val extraEnv    = libraryPathsEnv ++ programNameEnv ++ pythonExtraEnv
              val maybeResult = withNativeLauncher(
                builds,
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
                    launcher = launcher.toIO,
                    args = args,
                    logger = logger,
                    allowExecve = allowExecve,
                    extraEnv = extraEnv
                  )
                  Right((proc, None))
                }
              }
              value(maybeResult)
            case Platform.JVM =>
              def fwd(s: String): String  = s.replace('\\', '/')
              def base(s: String): String = fwd(s).replaceAll(".*/", "")
              runMode match {
                case RunMode.Default =>
                  val sourceFiles = builds.head.inputs.sourceFiles().map {
                    case s: ScalaFile    => fwd(s.path.toString)
                    case s: Script       => fwd(s.path.toString)
                    case s: MarkdownFile => fwd(s.path.toString)
                    case s: OnDisk       => fwd(s.path.toString)
                    case s               => s.getClass.getName
                  }.filter(_.nonEmpty).distinct
                  val sources     = sourceFiles.mkString(File.pathSeparator)
                  val sourceNames = sourceFiles.map(base(_)).mkString(File.pathSeparator)

                  val baseJavaProps = build.options.javaOptions.javaOpts.toSeq.map(_.value.value)
                    ++ Seq(s"-Dscala.sources=$sources", s"-Dscala.source.names=$sourceNames")
                  val setupPython = build.options.notForBloopOptions.doSetupPython.getOrElse(false)
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
                  if showCommand then
                    Left {
                      Runner.jvmCommand(
                        build.options.javaHome().value.javaCommand,
                        allJavaOpts,
                        builds.flatMap(_.fullClassPathMaybeAsJar(asJar)).distinct,
                        mainClass,
                        args,
                        extraEnv = pythonExtraEnv,
                        useManifest = build.options.notForBloopOptions.runWithManifest,
                        scratchDirOpt = scratchDirOpt
                      )
                    }
                  else {
                    val proc = Runner.runJvm(
                      javaCommand = build.options.javaHome().value.javaCommand,
                      javaArgs = allJavaOpts,
                      classPath = builds.flatMap(_.fullClassPathMaybeAsJar(asJar)).distinct,
                      mainClass = mainClass,
                      args = args,
                      logger = logger,
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
                      builds = builds,
                      mainClass = mainClass,
                      args = args,
                      submitArgs = mode.submitArgs,
                      logger = logger,
                      allowExecve = allowExecve,
                      showCommand = showCommand,
                      scratchDirOpt = scratchDirOpt
                    )
                  }
                case mode: RunMode.StandaloneSparkSubmit =>
                  value {
                    RunSpark.runStandalone(
                      builds = builds,
                      mainClass = mainClass,
                      args = args,
                      submitArgs = mode.submitArgs,
                      logger = logger,
                      allowExecve = allowExecve,
                      showCommand = showCommand,
                      scratchDirOpt = scratchDirOpt
                    )
                  }
                case RunMode.HadoopJar =>
                  value {
                    RunHadoop.run(
                      builds = builds,
                      mainClass = mainClass,
                      args = args,
                      logger = logger,
                      allowExecve = allowExecve,
                      showCommand = showCommand,
                      scratchDirOpt = scratchDirOpt
                    )
                  }
              }
          }
        }
      }
      .sequence
      .left.map(CompositeBuildException(_))
      .right.map(_.sequence.left.map(_.toSeq))
  }

  def withLinkedJs[T](
    builds: Seq[Build.Successful],
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
        builds = builds,
        dest = dest,
        mainClassOpt = mainClassOpt,
        addTestInitializer = addTestInitializer,
        config = config,
        fullOpt = fullOpt,
        noOpt = noOpt,
        logger = logger
      ).map(outputPath => f(outputPath))
    finally if (os.exists(dest)) os.remove(dest)
  }

  def withNativeLauncher[T](
    builds: Seq[Build.Successful],
    mainClass: String,
    logger: Logger
  )(f: os.Path => T): Either[BuildException, T] =
    Package.buildNative(
      builds = builds,
      mainClass = Some(mainClass),
      targetType = PackageType.Native.Application,
      destPath = None,
      logger = logger
    ).map(f)

  final class PythonDetectionError(cause: Throwable) extends BuildException(
        s"Error detecting Python environment: ${cause.getMessage}",
        cause = cause
      )

  extension [T](t: Try[T])
    def orPythonDetectionError: Either[PythonDetectionError, T] =
      t.toEither.left.map(new PythonDetectionError(_))
}
