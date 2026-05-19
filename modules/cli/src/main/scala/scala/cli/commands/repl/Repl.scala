package scala.cli.commands.repl
import caseapp.*
import caseapp.core.help.HelpFormat
import coursier.cache.FileCache
import coursier.error.ResolutionError
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
import scala.build.options.ScalacOpt.{filterScalacOptionKeys, noDashPrefixes}
import scala.build.options.{BuildOptions, JavaOpt, MaybeScalaVersion, ScalaVersionUtil, Scope}
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
import scala.jdk.CollectionConverters.*
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

  override def buildOptions(ops: ReplOptions): Some[BuildOptions] =
    Some(buildOptions0(
      ops,
      scala.cli.internal.Constants.maxAmmoniteScala3Version,
      scala.cli.internal.Constants.maxAmmoniteScala3LtsVersion
    ))
  private[commands] def buildOptions0(
    ops: ReplOptions,
    maxAmmoniteScalaVer: String,
    maxAmmoniteScalaLtsVer: String
  ): BuildOptions = {
    import ops.*
    import ops.sharedRepl.*

    val logger = ops.shared.logger
    if jshell.contains(true) && ammonite.contains(true)
    then throw new ConflictingReplBackendsError("--jshell cannot be used together with --ammonite")

    val ammoniteVersionOpt = ammoniteVersion.map(_.trim).filter(_.nonEmpty)
    val baseOptions        = shared.buildOptions(watchOptions = watch).orExit(logger)

    val maybeDowngradedScalaVersion = {
      val isDefaultAmmonite = ammonite.contains(true) && ammoniteVersionOpt.isEmpty
      extension (s: MaybeScalaVersion)
        private def isLtsAlias: Boolean =
          s.versionOpt.exists(v => ScalaVersionUtil.scala3Lts.contains(v.toLowerCase))
        private def isLts: Boolean =
          s.versionOpt.exists(_.startsWith(Constants.scala3LtsPrefix)) || isLtsAlias
      baseOptions.scalaOptions.scalaVersion match {
        case Some(s)
            if isDefaultAmmonite &&
            s.isLts &&
            (s
              .versionOpt.exists(_.coursierVersion > maxAmmoniteScalaLtsVer.coursierVersion) ||
            s.isLtsAlias) =>
          val versionString = s.versionOpt.filter(_ => !s.isLtsAlias).getOrElse(Constants.scala3Lts)
          logger.message(s"Scala $versionString is not yet supported with this version of Ammonite")
          logger.message(s"Defaulting to Scala $maxAmmoniteScalaLtsVer")
          Some(MaybeScalaVersion(maxAmmoniteScalaLtsVer))
        case None
            if isDefaultAmmonite &&
            maxAmmoniteScalaVer.coursierVersion < defaultScalaVersion.coursierVersion =>
          logger.message(
            s"Scala $defaultScalaVersion is not yet supported with this version of Ammonite"
          )
          logger.message(s"Defaulting to Scala $maxAmmoniteScalaVer")
          Some(MaybeScalaVersion(maxAmmoniteScalaVer))
        case s => s
      }
    }

    baseOptions.copy(
      scalaOptions = baseOptions.scalaOptions.copy(scalaVersion = maybeDowngradedScalaVersion),
      javaOptions = baseOptions.javaOptions.copy(
        javaOpts =
          baseOptions.javaOptions.javaOpts ++
            sharedJava.allJavaOpts.map(JavaOpt(_)).map(Positioned.commandLine)
      ),
      notForBloopOptions = baseOptions.notForBloopOptions.copy(
        replOptions = baseOptions.notForBloopOptions.replOptions.copy(
          useJshellOpt = jshell,
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
        // combining them may cause for ammonite args to be duplicated, so we're using the main scope's opts
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
    val shouldUseAmmonite = options.notForBloopOptions.replOptions.useAmmonite
    val explicitJshellOpt = options.notForBloopOptions.replOptions.useJshellOpt
    val isPureJavaProject =
      successfulBuilds.nonEmpty &&
      successfulBuilds.exists(_.sources.hasJava) &&
      !successfulBuilds.exists(_.sources.hasScala)
    val pureJavaInDefaultRepl =
      isPureJavaProject && explicitJshellOpt.contains(false) && !shouldUseAmmonite
    val shouldUseJshell =
      explicitJshellOpt.getOrElse(isPureJavaProject && !shouldUseAmmonite)
    val replBackend =
      if shouldUseJshell then ReplBackend.JShell
      else if shouldUseAmmonite then ReplBackend.Ammonite
      else ReplBackend.Default
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

    def ammoniteAdditionalArgs() = {
      val pythonPredef =
        if setupPython then
          """import me.shadaj.scalapy.py
            |import me.shadaj.scalapy.py.PyQuote
            |""".stripMargin
        else
          ""
      val pythonPredefArgs =
        if pythonPredef.isEmpty
        then Nil
        else Seq("--predef-code", pythonPredef)
      val replInitScriptPredefArgs =
        initScriptOpt.toSeq.flatMap(script => Seq("--predef-code", script))
      val replQuitAfterInitArgs =
        if quitAfterInit
        then Seq("--code", "")
        else Nil
      pythonPredefArgs ++ replInitScriptPredefArgs ++ replQuitAfterInitArgs ++
        options.notForBloopOptions.replOptions.ammoniteArgs
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
        finally if zf != null then zf.close()
    }
    val warnRootClasses = rootClasses.nonEmpty &&
      options.notForBloopOptions.replOptions.useAmmoniteOpt.contains(true)
    if warnRootClasses then
      logger.message(
        s"Warning: found classes defined in the root package (${rootClasses.mkString(", ")})." +
          " These will not be accessible from the REPL."
      )

    def maybeRunRepl(
      replArtifacts: ReplArtifacts,
      replArgs: Seq[String],
      extraEnv: Map[String, String] = Map.empty,
      extraProps: Map[String, String] = Map.empty
    ): Unit = {
      val isAmmonite = replArtifacts.replMainClass.startsWith("ammonite")
      if isAmmonite then
        replArgs
          .map(_.noDashPrefixes)
          .filter(ScalacOptions.replExecuteScriptOptions.contains)
          .foreach(arg =>
            logger.message(
              s"The '--$arg' option is not supported with Ammonite. Did you mean to use '--ammonite-arg -c' to execute a script?"
            )
          )
      if dryRun then logger.message("Dry run, not running REPL.")
      else {
        val depClassPathArgs: Seq[String] =
          if !isAmmonite && (mainJarsOrClassDirs.nonEmpty || replArtifacts.depsClassPath.nonEmpty)
          then
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
    def ammoniteArtifacts(): Either[BuildException, ReplArtifacts] =
      ReplArtifacts.ammonite(
        scalaParams = scalaParams,
        ammoniteVersion =
          options.notForBloopOptions.replOptions.ammoniteVersion(scalaParams.scalaVersion, logger),
        dependencies = allArtifacts.flatMap(_.userDependencies),
        extraClassPath = allArtifacts.flatMap(_.extraClassPath),
        extraSourceJars = allArtifacts.flatMap(_.extraSourceJars),
        extraRepositories = value(options.finalRepositories),
        logger = logger,
        cache = cache,
        addScalapy =
          if setupPython
          then Some(options.notForBloopOptions.scalaPyVersion.getOrElse(Constants.scalaPyVersion))
          else None
      ).left.map {
        case FetchingDependenciesError(e: ResolutionError.CantDownloadModule, positions)
            if shouldUseAmmonite &&
            e.module.name.value == s"ammonite_${scalaParams.scalaVersion}" =>
          CantDownloadAmmoniteError(
            e.versionConstraint.asString,
            scalaParams.scalaVersion,
            e,
            positions
          )
        case other => other
      }

    replBackend match {
      case ReplBackend.JShell =>
        val javaHomeInfo   = options.javaHome().value
        val jshellCommand0 = value(
          JShellRunner.commandFor(
            javaHomeInfo = javaHomeInfo,
            javaOpts = scalapyJavaOpts ++ options.javaOptions.javaOpts.toSeq.map(_.value.value),
            classPath = mainJarsOrClassDirs ++ allArtifacts.flatMap(_.classPath).distinct,
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

      case ReplBackend.Ammonite =>
        runMode match {
          case RunMode.Default =>
            val replArtifacts = value(ammoniteArtifacts())
            val ammoniteArgs  = ammoniteAdditionalArgs() ++ programArgs
            maybeRunRepl(replArtifacts, ammoniteArgs)
        }

      case ReplBackend.Default =>
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
  }

  private enum ReplBackend {
    case Default, Ammonite, JShell
  }

  final class ConflictingReplBackendsError(message0: String) extends BuildException(message0)
  final class ReplInitScriptError(message0: String, cause0: Throwable = null)
      extends BuildException(message0, cause = cause0)
  final class ReplError(retCode: Int)
      extends BuildException(s"Failed to run REPL (exit code: $retCode)")
}
