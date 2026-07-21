package scala.cli.commands.repl
import caseapp.*
import caseapp.core.help.HelpFormat
import coursier.cache.FileCache
import dependency.*

import java.io.File

import scala.build.*
import scala.build.EitherCps.{either, value}
import scala.build.errors.{BuildException, MultipleScalaVersionsError}
import scala.build.input.Inputs
import scala.build.internal.{Constants, Runner}
import scala.build.options.ScalacOpt.{filterScalacOptionKeys, noDashPrefixes}
import scala.build.options.{BuildOptions, JavaOpt, Scope}
import scala.build.postprocessing.{SlothAgent, SlothPatcher}
import scala.cli.CurrentParams
import scala.cli.commands.run.Run.{createPythonInstance, orPythonDetectionError, pythonPathEnv}
import scala.cli.commands.run.RunMode
import scala.cli.commands.shared.{HelpCommandGroup, HelpGroup, ScalacOptions, SharedOptions}
import scala.cli.commands.util.BuildCommandHelpers
import scala.cli.commands.util.ScalacOptionsUtil.*
import scala.cli.commands.{ScalaCommand, WatchUtil}
import scala.cli.config.Keys
import scala.cli.packaging.Library
import scala.cli.util.ArgHelpers.*
import scala.cli.util.ConfigDbUtils
import scala.util.Properties

object Repl extends ScalaCommand[ReplOptions] with BuildCommandHelpers {
  override def group: String           = HelpCommandGroup.Main.toString
  override def scalaSpecificationLevel = SpecificationLevel.MUST
  override def helpFormat: HelpFormat  = super.helpFormat
    .withHiddenGroup(HelpGroup.Watch)
    .withPrimaryGroup(HelpGroup.Repl)
  override def names: List[List[String]] = List(
    List("repl"),
    List("console")
  )
  override def sharedOptions(options: ReplOptions): Option[SharedOptions] = Some(options.shared)

  override def buildOptions(ops: ReplOptions): Some[BuildOptions] = {
    import ops.*
    import ops.sharedRepl.*

    val logger      = ops.shared.logger
    val baseOptions = shared.buildOptions(watchOptions = watch).orExit(logger)

    Some(
      baseOptions.copy(
        javaOptions = baseOptions.javaOptions.copy(
          javaOpts =
            baseOptions.javaOptions.javaOpts ++
              sharedJava.allJavaOpts.map(JavaOpt(_)).map(Positioned.commandLine)
        ),
        notForBloopOptions = baseOptions.notForBloopOptions.copy(
          replOptions = baseOptions.notForBloopOptions.replOptions.copy(
            useJshellOpt = jshell
          ),
          addRunnerDependencyOpt = baseOptions.notForBloopOptions.addRunnerDependencyOpt
            .orElse(Some(false))
        )
      )
    )
  }

  private def runMode(options: ReplOptions): RunMode.HasRepl =
    RunMode.Default

  override def runCommand(options: ReplOptions, args: RemainingArgs, logger: Logger): Unit = {
    val initialBuildOptions = buildOptionsOrExit(options)
    val initScriptOpt       =
      options.sharedRepl.replInitScriptFile
        .map(_.trim)
        .filter(_.nonEmpty)
        .map(path => readInitScriptFile(path).orExit(logger))
    val quitAfterInit =
      options.shared.scalac.scalacOption
        .toScalacOptShadowingSeq
        .filterScalacOptionKeys(_.noDashPrefixes == ScalacOptions.replQuitAfterInit)
        .keys.nonEmpty
    def default = Inputs.default().getOrElse {
      Inputs.empty(Os.pwd, options.shared.markdown.enableMarkdown)
    }
    val inputs =
      options.shared.inputs(args.remaining, defaultInputs = () => Some(default)).orExit(logger)
    val programArgs = args.unparsed
    CurrentParams.workspaceOpt = Some(inputs.workspace)

    val threads = BuildThreads.create()
    // compilerMaker should be a lazy val to prevent download a JAVA 17 for bloop when users run the repl without sources
    lazy val compilerMaker = options.shared.compilerMaker(threads)

    def doRunRepl(
      buildOptions: BuildOptions,
      initScriptOpt: Option[String],
      quitAfterInit: Boolean,
      allArtifacts: Seq[Artifacts],
      mainJarsOrClassDirs: Seq[os.Path],
      allowExit: Boolean,
      runMode: RunMode.HasRepl,
      successfulBuilds: Seq[Build.Successful]
    ): Unit = {
      val res = runRepl(
        options = buildOptions,
        initScriptOpt = initScriptOpt,
        quitAfterInit = quitAfterInit,
        programArgs = programArgs,
        allArtifacts = allArtifacts,
        mainJarsOrClassDirs = mainJarsOrClassDirs,
        logger = logger,
        allowExit = allowExit,
        dryRun = options.sharedRepl.replDryRun,
        runMode = runMode,
        successfulBuilds = successfulBuilds
      )
      res match {
        case Left(ex)  => if allowExit then logger.exit(ex) else logger.log(ex)
        case Right(()) =>
      }
    }
    def doRunReplFromBuild(
      builds: Seq[Build.Successful],
      initScriptOpt: Option[String],
      quitAfterInit: Boolean,
      allowExit: Boolean,
      runMode: RunMode.HasRepl,
      asJar: Boolean
    ): Unit = {
      doRunRepl(
        // build options should be the same for both scopes
        // build options should be the same for both scopes; use the main scope's opts
        buildOptions = builds.head.options,
        initScriptOpt = initScriptOpt,
        quitAfterInit = quitAfterInit,
        allArtifacts = builds.map(_.artifacts),
        mainJarsOrClassDirs =
          if asJar then Seq(Library.libraryJar(builds)) else builds.map(_.output),
        allowExit = allowExit,
        runMode = runMode,
        successfulBuilds = builds
      )
    }

    val cross                 = options.sharedRepl.compileCross.cross.getOrElse(false)
    val configDb              = ConfigDbUtils.configDb.orExit(logger)
    val actionableDiagnostics =
      options.shared.logging.verbosityOptions.actions.orElse(
        configDb.get(Keys.actions).getOrElse(None)
      )

    val shouldBuildTestScope = options.shared.scope.test.getOrElse(false)
    if inputs.isEmpty then {
      val allArtifacts =
        Seq(initialBuildOptions.artifacts(logger, Scope.Main).orExit(logger)) ++
          (if shouldBuildTestScope
           then Seq(initialBuildOptions.artifacts(logger, Scope.Test).orExit(logger))
           else Nil)
      // synchronizing, so that multiple presses to enter (handled by WatchUtil.waitForCtrlC)
      // don't try to run repls in parallel
      val lock       = new Object
      def runThing() = lock.synchronized {
        doRunRepl(
          buildOptions = initialBuildOptions,
          initScriptOpt = initScriptOpt,
          quitAfterInit = quitAfterInit,
          allArtifacts = allArtifacts,
          mainJarsOrClassDirs = Seq.empty,
          allowExit = !options.sharedRepl.watch.watchMode,
          runMode = runMode(options),
          successfulBuilds = Seq.empty
        )
      }
      runThing()
      if options.sharedRepl.watch.watchMode then {
        // nothing to watch, just wait for Ctrl+C
        WatchUtil.printWatchMessage()
        WatchUtil.waitForCtrlC(() => runThing())
      }
    }
    else if options.sharedRepl.watch.watchMode then {
      val watcher = Build.watch(
        inputs,
        initialBuildOptions,
        compilerMaker,
        None,
        logger,
        crossBuilds = cross,
        buildTests = shouldBuildTestScope,
        partial = None,
        actionableDiagnostics = actionableDiagnostics,
        postAction = () => WatchUtil.printWatchMessage()
      ) { res =>
        for (builds <- res.orReport(logger))
          postBuild(builds, allowExit = false) {
            successfulBuilds =>
              doRunReplFromBuild(
                successfulBuilds,
                initScriptOpt = initScriptOpt,
                quitAfterInit = quitAfterInit,
                allowExit = false,
                runMode = runMode(options),
                asJar = options.shared.asJar
              )
          }
      }
      try WatchUtil.waitForCtrlC(() => watcher.schedule())
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
          buildTests = shouldBuildTestScope,
          partial = None,
          actionableDiagnostics = actionableDiagnostics
        )
          .orExit(logger)
      postBuild(builds, allowExit = false) {
        successfulBuilds =>
          doRunReplFromBuild(
            successfulBuilds,
            initScriptOpt = initScriptOpt,
            quitAfterInit = quitAfterInit,
            allowExit = true,
            runMode = runMode(options),
            asJar = options.shared.asJar
          )
      }
    }
  }

  def postBuild(builds: Builds, allowExit: Boolean)(f: Seq[Build.Successful] => Unit): Unit = {
    if builds.anyBuildFailed then {
      System.err.println("Compilation failed")
      if allowExit then sys.exit(1)
    }
    else if builds.anyBuildCancelled then {
      System.err.println("Build cancelled")
      if allowExit then sys.exit(1)
    }
    else f(builds.builds.sortBy(_.scope).map(_.asInstanceOf[Build.Successful]))
  }

  private def maybeAdaptForWindows(args: Seq[String]): Seq[String] =
    if Properties.isWin then
      args.map { a =>
        if a.exists(c => c.isWhitespace || c == '"')
        then "\"" + a.replace("\"", "\\\"") + "\""
        else a
      }
    else
      args

  private[commands] def readInitScriptFile(file: String): Either[BuildException, String] = {
    val pathEither: Either[BuildException, os.Path] =
      try Right(os.Path(file, os.pwd))
      catch {
        case e: IllegalArgumentException =>
          Left(ReplInitScriptError(s"Invalid REPL init script file path: $file", e))
      }
    pathEither.flatMap { path =>
      if !os.exists(path) then
        Left(ReplInitScriptError(s"REPL init script file not found: $path"))
      else if os.isDir(path) then
        Left(ReplInitScriptError(s"REPL init script file is a directory: $path"))
      else
        try Right(os.read(path))
        catch {
          case e: Exception =>
            Left(ReplInitScriptError(
              s"Error reading REPL init script file $path: ${e.getMessage}",
              e
            ))
        }
    }
  }

  private def runRepl(
    options: BuildOptions,
    initScriptOpt: Option[String],
    quitAfterInit: Boolean,
    programArgs: Seq[String],
    allArtifacts: Seq[Artifacts],
    mainJarsOrClassDirs: Seq[os.Path],
    logger: Logger,
    allowExit: Boolean,
    dryRun: Boolean,
    runMode: RunMode.HasRepl,
    successfulBuilds: Seq[Build.Successful]
  ): Either[BuildException, Unit] = either {
    val setupPython = options.notForBloopOptions.python.getOrElse(false)

    val cache             = options.internal.cache.getOrElse(FileCache())
    val explicitJshellOpt = options.notForBloopOptions.replOptions.useJshellOpt
    val isPureJavaProject =
      successfulBuilds.nonEmpty &&
      successfulBuilds.exists(_.sources.hasJava) &&
      !successfulBuilds.exists(_.sources.hasScala)
    val pureJavaInDefaultRepl =
      isPureJavaProject && explicitJshellOpt.contains(false)
    val shouldUseJshell =
      explicitJshellOpt.getOrElse(isPureJavaProject)
    if pureJavaInDefaultRepl then
      logger.message(
        "Detected a pure-Java project, but --jshell=false was passed; using the default Scala REPL instead of JShell."
      )

    val scalaParams: ScalaParameters = value {
      val distinctScalaParams = allArtifacts.flatMap(_.scalaOpt).map(_.params).distinct
      if distinctScalaParams.isEmpty then
        Right(ScalaParameters(Constants.defaultScalaVersion))
      else if distinctScalaParams.length == 1 then
        Right(distinctScalaParams.head)
      else Left(MultipleScalaVersionsError(distinctScalaParams.map(_.scalaVersion)))
    }

    val (scalapyJavaOpts, scalapyExtraEnv) =
      if setupPython then {
        val props = value {
          val python       = value(createPythonInstance().orPythonDetectionError)
          val propsOrError = python.scalapyProperties
          logger.debug(s"Python Java properties: $propsOrError")
          propsOrError.orPythonDetectionError
        }
        val props0 = props.toVector.sorted.map {
          case (k, v) => s"-D$k=$v"
        }
        // Putting current dir in PYTHONPATH, see
        // https://github.com/VirtusLab/scala-cli/pull/1616#issuecomment-1333283174
        // for context.
        val dirs = successfulBuilds.map(_.inputs.workspace) ++ Seq(os.pwd)
        (props0, pythonPathEnv(dirs*))
      }
      else
        (Nil, Map.empty[String, String])

    val pythonReplArgs =
      if setupPython && scalaParams.scalaVersion.startsWith("2.13.")
      then Seq("-Yimports:java.lang,scala,scala.Predef,me.shadaj.scalapy")
      else Nil
    val additionalArgs =
      pythonReplArgs ++ options.scalaOptions.scalacOptions.toSeq.map(_.value.value)

    val javaCommand = options.javaHome().value.javaCommand

    val slothAgentJavaOpts = value(SlothAgent.javaAgentArgs(options, logger))

    def patchClassPath(classPath: Seq[os.Path]): Seq[os.Path] =
      value(SlothPatcher.transformClassPath(
        classPath,
        options,
        logger,
        patchProjectClassDirs = SlothPatcher.shouldPatchProjectClasses(successfulBuilds)
      ))

    def maybeRunRepl(
      replArtifacts: ReplArtifacts,
      replArgs: Seq[String],
      extraEnv: Map[String, String] = Map.empty,
      extraProps: Map[String, String] = Map.empty
    ): Unit = {
      if dryRun then logger.message("Dry run, not running REPL.")
      else {
        val depClassPath  = patchClassPath(mainJarsOrClassDirs ++ replArtifacts.depsClassPath)
        val replClassPath = patchClassPath(replArtifacts.replClassPath)
        val depClassPathArgs: Seq[String] =
          if depClassPath.nonEmpty
          then
            Seq(
              "-classpath",
              depClassPath.map(_.toString).mkString(File.pathSeparator)
            )
          else Nil
        val retCode = Runner.runJvm(
          javaCommand = javaCommand,
          javaArgs = slothAgentJavaOpts ++
            scalapyJavaOpts ++
            replArtifacts.replJavaOpts ++
            options.javaOptions.javaOpts.toSeq.map(_.value.value) ++
            extraProps.toVector.sorted.map { case (k, v) => s"-D$k=$v" },
          classPath = replClassPath,
          mainClass = replArtifacts.replMainClass,
          args = maybeAdaptForWindows(depClassPathArgs ++ replArgs),
          logger = logger,
          allowExecve = allowExit,
          extraEnv = scalapyExtraEnv ++ extraEnv
        ).waitFor()
        if retCode != 0 then value(Left(new ReplError(retCode)))
      }
    }

    def defaultArtifacts(): Either[BuildException, ReplArtifacts] = either {
      value {
        ReplArtifacts.default(
          scalaParams = scalaParams,
          dependencies = allArtifacts.flatMap(_.userDependencies).distinct,
          extraClassPath = allArtifacts.flatMap(_.extraClassPath).distinct,
          logger = logger,
          cache = cache,
          repositories = value(options.finalRepositories),
          addScalapy =
            if setupPython
            then Some(options.notForBloopOptions.scalaPyVersion.getOrElse(Constants.scalaPyVersion))
            else None,
          javaVersion = options.javaHome().value.version
        )
      }
    }
    if shouldUseJshell then
      val javaHomeInfo    = options.javaHome().value
      val jshellClassPath =
        patchClassPath(mainJarsOrClassDirs ++ allArtifacts.flatMap(_.classPath).distinct)
      val jshellCommand0 = value(
        JShellRunner.commandFor(
          javaHomeInfo = javaHomeInfo,
          javaOpts = slothAgentJavaOpts ++
            scalapyJavaOpts ++ options.javaOptions.javaOpts.toSeq.map(_.value.value),
          classPath = jshellClassPath,
          programArgs = programArgs,
          initScriptOpt = initScriptOpt,
          quitAfterInit = quitAfterInit,
          currentEnv = sys.env
        )
      )
      val jshellCommand =
        jshellCommand0.copy(extraEnv = jshellCommand0.extraEnv ++ scalapyExtraEnv)
      value(
        JShellRunner.run(
          jshellCommand,
          logger,
          allowExecve = allowExit,
          dryRun = dryRun
        )
      )
    else
      runMode match {
        case RunMode.Default =>
          val replArtifacts  = value(defaultArtifacts())
          val initScriptArgs =
            initScriptOpt.toSeq.map { script =>
              s"--${ScalacOptions.replInitScript}:$script"
            }
          val defaultArgs = additionalArgs ++ initScriptArgs ++ programArgs
          maybeRunRepl(replArtifacts, defaultArgs)
      }
  }
  final class ReplInitScriptError(message0: String, cause0: Throwable = null)
      extends BuildException(message0, cause = cause0)
  final class ReplError(retCode: Int)
      extends BuildException(s"Failed to run REPL (exit code: $retCode)")
}
