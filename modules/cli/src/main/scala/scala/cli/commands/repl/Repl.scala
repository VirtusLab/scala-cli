package scala.cli.commands.repl

import ai.kien.python.Python
import caseapp.*
import caseapp.core.help.HelpFormat
import coursier.cache.FileCache
import coursier.error.{FetchError, ResolutionError}
import dependency.*

import java.io.File
import java.util.zip.ZipFile

import scala.build.*
import scala.build.EitherCps.{either, value}
import scala.build.errors.{
  BuildException,
  CantDownloadAmmoniteError,
  FetchingDependenciesError,
  MultipleScalaVersionsError
}
import scala.build.input.Inputs
import scala.build.internal.{Constants, Runner}
import scala.build.options.{BuildOptions, JavaOpt, MaybeScalaVersion, Scope}
import scala.cli.commands.publish.ConfigUtil.*
import scala.cli.commands.run.Run.{
  maybePrintSimpleScalacOutput,
  orPythonDetectionError,
  pythonPathEnv
}
import scala.cli.commands.run.RunMode
import scala.cli.commands.shared.{HelpCommandGroup, HelpGroup, SharedOptions}
import scala.cli.commands.util.BuildCommandHelpers
import scala.cli.commands.{ScalaCommand, WatchUtil}
import scala.cli.config.{ConfigDb, Keys}
import scala.cli.packaging.Library
import scala.cli.util.ArgHelpers.*
import scala.cli.util.ConfigDbUtils
import scala.cli.{CurrentParams, ScalaCli}
import scala.jdk.CollectionConverters.*
import scala.util.Properties

object Repl extends ScalaCommand[ReplOptions] with BuildCommandHelpers {
  override def group: String           = HelpCommandGroup.Main.toString
  override def scalaSpecificationLevel = SpecificationLevel.MUST
  override def helpFormat: HelpFormat = super.helpFormat
    .withHiddenGroup(HelpGroup.Watch)
    .withPrimaryGroup(HelpGroup.Repl)
  override def names: List[List[String]] = List(
    List("repl"),
    List("console")
  )
  override def sharedOptions(options: ReplOptions): Option[SharedOptions] = Some(options.shared)

  override def buildOptions(ops: ReplOptions): Some[BuildOptions] =
    Some(buildOptions0(ops, scala.cli.internal.Constants.maxAmmoniteScala3Version))
  private[commands] def buildOptions0(
    ops: ReplOptions,
    maxAmmoniteScalaVer: String
  ): BuildOptions = {
    import ops.*
    import ops.sharedRepl.*

    val logger = ops.shared.logger

    val ammoniteVersionOpt = ammoniteVersion.map(_.trim).filter(_.nonEmpty)
    val baseOptions        = shared.buildOptions().orExit(logger)
    baseOptions.copy(
      scalaOptions = baseOptions.scalaOptions.copy(
        scalaVersion = baseOptions.scalaOptions.scalaVersion
          .orElse {
            val shouldDowngrade = {
              def needsDowngradeForAmmonite = {
                import coursier.core.Version
                Version(maxAmmoniteScalaVer) < Version(defaultScalaVersion)
              }
              ammonite.contains(true) &&
              ammoniteVersionOpt.isEmpty &&
              needsDowngradeForAmmonite
            }
            if (shouldDowngrade) {
              logger.message(
                s"Scala $defaultScalaVersion is not yet supported with this version of Ammonite"
              )
              logger.message(s"Defaulting to Scala $maxAmmoniteScalaVer")
              Some(MaybeScalaVersion(maxAmmoniteScalaVer))
            }
            else None
          }
      ),
      javaOptions = baseOptions.javaOptions.copy(
        javaOpts =
          baseOptions.javaOptions.javaOpts ++
            sharedJava.allJavaOpts.map(JavaOpt(_)).map(Positioned.commandLine)
      ),
      notForBloopOptions = baseOptions.notForBloopOptions.copy(
        replOptions = baseOptions.notForBloopOptions.replOptions.copy(
          useAmmoniteOpt = ammonite,
          ammoniteVersionOpt = ammoniteVersion.map(_.trim).filter(_.nonEmpty),
          ammoniteArgs = ammoniteArg
        ),
        addRunnerDependencyOpt = baseOptions.notForBloopOptions.addRunnerDependencyOpt
          .orElse(Some(false))
      )
    )
  }

  private def runMode(options: ReplOptions): RunMode.HasRepl =
    RunMode.Default

  override def runCommand(options: ReplOptions, args: RemainingArgs, logger: Logger): Unit = {
    val initialBuildOptions = buildOptionsOrExit(options)
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

    val directories = Directories.directories

    def doRunRepl(
      buildOptions: BuildOptions,
      allArtifacts: Seq[Artifacts],
      mainJarsOrClassDirs: Seq[os.Path],
      allowExit: Boolean,
      runMode: RunMode.HasRepl,
      successfulBuilds: Seq[Build.Successful]
    ): Unit = {
      val res = runRepl(
        options = buildOptions,
        programArgs = programArgs,
        allArtifacts = allArtifacts,
        mainJarsOrClassDirs = mainJarsOrClassDirs,
        directories = directories,
        logger = logger,
        allowExit = allowExit,
        dryRun = options.sharedRepl.replDryRun,
        runMode = runMode,
        successfulBuilds = successfulBuilds
      )
      res match {
        case Left(ex) =>
          if (allowExit) logger.exit(ex)
          else logger.log(ex)
        case Right(()) =>
      }
    }
    def doRunReplFromBuild(
      builds: Seq[Build.Successful],
      allowExit: Boolean,
      runMode: RunMode.HasRepl,
      asJar: Boolean
    ): Unit = {
      doRunRepl(
        // build options should be the same for both scopes
        // combining them may cause for ammonite args to be duplicated, so we're using the main scope's opts
        buildOptions = builds.head.options,
        allArtifacts = builds.map(_.artifacts),
        mainJarsOrClassDirs =
          if (asJar) builds.map(Library.libraryJar(_)) else builds.map(_.output),
        allowExit = allowExit,
        runMode = runMode,
        successfulBuilds = builds
      )
    }

    val cross    = options.sharedRepl.compileCross.cross.getOrElse(false)
    val configDb = ConfigDbUtils.configDb.orExit(logger)
    val actionableDiagnostics =
      options.shared.logging.verbosityOptions.actions.orElse(
        configDb.get(Keys.actions).getOrElse(None)
      )

    if (inputs.isEmpty) {
      val allArtifacts =
        Seq(initialBuildOptions.artifacts(logger, Scope.Main).orExit(logger)) ++
          (if options.sharedRepl.scope.test
           then Seq(initialBuildOptions.artifacts(logger, Scope.Test).orExit(logger))
           else Nil)
      // synchronizing, so that multiple presses to enter (handled by WatchUtil.waitForCtrlC)
      // don't try to run repls in parallel
      val lock = new Object
      def runThing() = lock.synchronized {
        doRunRepl(
          buildOptions = initialBuildOptions,
          allArtifacts = allArtifacts,
          mainJarsOrClassDirs = Seq.empty,
          allowExit = !options.sharedRepl.watch.watchMode,
          runMode = runMode(options),
          successfulBuilds = Seq.empty
        )
      }
      runThing()
      if (options.sharedRepl.watch.watchMode) {
        // nothing to watch, just wait for Ctrl+C
        WatchUtil.printWatchMessage()
        WatchUtil.waitForCtrlC(() => runThing())
      }
    }
    else if (options.sharedRepl.watch.watchMode) {
      val watcher = Build.watch(
        inputs,
        initialBuildOptions,
        compilerMaker,
        None,
        logger,
        crossBuilds = cross,
        buildTests = options.sharedRepl.scope.test,
        partial = None,
        actionableDiagnostics = actionableDiagnostics,
        postAction = () => WatchUtil.printWatchMessage()
      ) { res =>
        for (builds <- res.orReport(logger))
          postBuild(builds, allowExit = false) {
            successfulBuilds =>
              doRunReplFromBuild(
                successfulBuilds,
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
          buildTests = options.sharedRepl.scope.test,
          partial = None,
          actionableDiagnostics = actionableDiagnostics
        )
          .orExit(logger)
      postBuild(builds, allowExit = false) {
        successfulBuilds =>
          doRunReplFromBuild(
            successfulBuilds,
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
    if (Properties.isWin)
      args.map { a =>
        if (a.contains(" ")) "\"" + a.replace("\"", "\\\"") + "\""
        else a
      }
    else
      args

  private def runRepl(
    options: BuildOptions,
    programArgs: Seq[String],
    allArtifacts: Seq[Artifacts],
    mainJarsOrClassDirs: Seq[os.Path],
    directories: scala.build.Directories,
    logger: Logger,
    allowExit: Boolean,
    dryRun: Boolean,
    runMode: RunMode.HasRepl,
    successfulBuilds: Seq[Build.Successful]
  ): Either[BuildException, Unit] = either {

    val setupPython = options.notForBloopOptions.python.getOrElse(false)

    val cache             = options.internal.cache.getOrElse(FileCache())
    val shouldUseAmmonite = options.notForBloopOptions.replOptions.useAmmonite

    val scalaParams: ScalaParameters = value {
      val distinctScalaParams = allArtifacts.flatMap(_.scalaOpt).map(_.params).distinct
      if distinctScalaParams.isEmpty then
        Right(ScalaParameters(Constants.defaultScalaVersion))
      else if distinctScalaParams.length == 1 then
        Right(distinctScalaParams.head)
      else Left(MultipleScalaVersionsError(distinctScalaParams.map(_.scalaVersion)))
    }

    val (scalapyJavaOpts, scalapyExtraEnv) =
      if (setupPython) {
        val props = value {
          val python       = Python()
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
        (props0, pythonPathEnv(dirs: _*))
      }
      else
        (Nil, Map.empty[String, String])

    def additionalArgs = {
      val pythonArgs =
        if (setupPython && scalaParams.scalaVersion.startsWith("2.13."))
          Seq("-Yimports:java.lang,scala,scala.Predef,me.shadaj.scalapy")
        else
          Nil
      pythonArgs ++ options.scalaOptions.scalacOptions.toSeq.map(_.value.value)
    }

    def ammoniteAdditionalArgs() = {
      val pythonPredef =
        if (setupPython)
          """import me.shadaj.scalapy.py
            |import me.shadaj.scalapy.py.PyQuote
            |""".stripMargin
        else
          ""
      val predefArgs =
        if (pythonPredef.isEmpty) Nil
        else Seq("--predef-code", pythonPredef)
      predefArgs ++ options.notForBloopOptions.replOptions.ammoniteArgs
    }

    // TODO Warn if some entries of artifacts.classPath were evicted in replArtifacts.replClassPath
    //      (should be artifacts whose version was bumped by Ammonite).

    // TODO Find the common namespace of all user classes, and import it all in the Ammonite session.

    // TODO Allow to disable printing the welcome banner and the "Loading..." message in Ammonite.

    val rootClasses = mainJarsOrClassDirs.flatMap {
      case dir if os.isDir(dir) =>
        os.list(dir)
          .filter(_.last.endsWith(".class"))
          .filter(os.isFile(_)) // just in case
          .map(_.last.stripSuffix(".class"))
          .sorted
      case jar =>
        var zf: ZipFile = null
        try {
          zf = new ZipFile(jar.toIO)
          zf.entries()
            .asScala
            .map(_.getName)
            .filter(!_.contains("/"))
            .filter(_.endsWith(".class"))
            .map(_.stripSuffix(".class"))
            .toVector
            .sorted
        }
        finally
          if (zf != null)
            zf.close()
    }
    val warnRootClasses = rootClasses.nonEmpty &&
      options.notForBloopOptions.replOptions.useAmmoniteOpt.contains(true)
    if (warnRootClasses)
      logger.message(
        s"Warning: found classes defined in the root package (${rootClasses.mkString(", ")})." +
          " These will not be accessible from the REPL."
      )

    def maybeRunRepl(
      replArtifacts: ReplArtifacts,
      replArgs: Seq[String],
      extraEnv: Map[String, String] = Map.empty,
      extraProps: Map[String, String] = Map.empty
    ): Unit =
      if (dryRun)
        logger.message("Dry run, not running REPL.")
      else {
        val isAmmonite = replArtifacts.replMainClass.startsWith("ammonite")
        val depClassPathArgs: Seq[String] =
          if replArtifacts.depsClassPath.nonEmpty && !isAmmonite then
            Seq(
              "-classpath",
              (mainJarsOrClassDirs ++ replArtifacts.depsClassPath)
                .map(_.toString).mkString(File.pathSeparator)
            )
          else Nil
        val replLauncherClasspath =
          if isAmmonite then mainJarsOrClassDirs ++ replArtifacts.replClassPath
          else replArtifacts.replClassPath
        val retCode = Runner.runJvm(
          javaCommand = options.javaHome().value.javaCommand,
          javaArgs = scalapyJavaOpts ++
            replArtifacts.replJavaOpts ++
            options.javaOptions.javaOpts.toSeq.map(_.value.value) ++
            extraProps.toVector.sorted.map { case (k, v) => s"-D$k=$v" },
          classPath = replLauncherClasspath,
          mainClass = replArtifacts.replMainClass,
          args = maybeAdaptForWindows(depClassPathArgs ++ replArgs),
          logger = logger,
          allowExecve = allowExit,
          extraEnv = scalapyExtraEnv ++ extraEnv
        ).waitFor()
        if (retCode != 0)
          value(Left(new ReplError(retCode)))
      }

    def defaultArtifacts(): Either[BuildException, ReplArtifacts] = either {
      value {
        ReplArtifacts.default(
          scalaParams,
          allArtifacts.flatMap(_.userDependencies).distinct,
          allArtifacts.flatMap(_.extraClassPath).distinct,
          logger,
          cache,
          value(options.finalRepositories),
          addScalapy =
            if (setupPython)
              Some(options.notForBloopOptions.scalaPyVersion.getOrElse(Constants.scalaPyVersion))
            else None
        )
      }
    }
    def ammoniteArtifacts(): Either[BuildException, ReplArtifacts] =
      ReplArtifacts.ammonite(
        scalaParams,
        options.notForBloopOptions.replOptions.ammoniteVersion(scalaParams.scalaVersion, logger),
        allArtifacts.flatMap(_.userDependencies),
        allArtifacts.flatMap(_.extraClassPath),
        allArtifacts.flatMap(_.extraSourceJars),
        value(options.finalRepositories),
        logger,
        cache,
        directories,
        addScalapy =
          if (setupPython)
            Some(options.notForBloopOptions.scalaPyVersion.getOrElse(Constants.scalaPyVersion))
          else None
      ).left.map {
        case FetchingDependenciesError(e: ResolutionError.CantDownloadModule, positions)
            if shouldUseAmmonite && e.module.name.value == s"ammonite_${scalaParams.scalaVersion}" =>
          CantDownloadAmmoniteError(e.version, scalaParams.scalaVersion, e, positions)
        case other => other
      }

    if (shouldUseAmmonite)
      runMode match {
        case RunMode.Default =>
          val replArtifacts = value(ammoniteArtifacts())
          val replArgs      = ammoniteAdditionalArgs() ++ programArgs
          maybeRunRepl(replArtifacts, replArgs)
      }
    else
      runMode match {
        case RunMode.Default =>
          val replArtifacts = value(defaultArtifacts())
          val replArgs      = additionalArgs ++ programArgs
          maybeRunRepl(replArtifacts, replArgs)
      }
  }

  final class ReplError(retCode: Int)
      extends BuildException(s"Failed to run REPL (exit code: $retCode)")
}
