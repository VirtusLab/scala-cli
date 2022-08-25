package scala.cli.commands.repl

import ai.kien.python.Python
import caseapp.*
import caseapp.core.help.HelpFormat
import coursier.cache.FileCache
import coursier.error.{FetchError, ResolutionError}
import dependency.*

import java.math.BigInteger
import java.security.SecureRandom

import scala.build.EitherCps.{either, value}
import scala.build.*
import scala.build.errors.{BuildException, CantDownloadAmmoniteError, FetchingDependenciesError}
import scala.build.input.Inputs
import scala.build.internal.{Constants, Runner}
import scala.build.options.{BuildOptions, JavaOpt, MaybeScalaVersion, Scope}
import scala.cli.CurrentParams
import scala.cli.commands.publish.ConfigUtil.*
import scala.cli.commands.run.Run.{
  maybePrintSimpleScalacOutput,
  orPythonDetectionError,
  pythonPathEnv
}
import scala.cli.commands.run.RunMode
import scala.cli.commands.shared.{HelpCommandGroup, HelpGroup, SharedOptions}
import scala.cli.commands.util.RunSpark
import scala.cli.commands.{ScalaCommand, WatchUtil}
import scala.cli.config.{ConfigDb, Keys}
import scala.cli.util.ArgHelpers.*
import scala.util.Properties

object Repl extends ScalaCommand[ReplOptions] {
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

    val ammoniteVersionOpt = ammoniteVersion.map(_.trim).filter(_.nonEmpty)

    val logger = ops.shared.logger

    val spark = runMode(ops) match {
      case _: RunMode.Spark => true
      case RunMode.Default  => false
    }

    val baseOptions = shared.buildOptions().orExit(logger)
    baseOptions.copy(
      scalaOptions = baseOptions.scalaOptions.copy(
        scalaVersion = baseOptions.scalaOptions.scalaVersion
          .orElse(if (spark) Some(MaybeScalaVersion("2.12")) else None)
          .orElse {
            val defaultScalaVer = scala.build.internal.Constants.defaultScalaVersion
            val shouldDowngrade = {
              def needsDowngradeForAmmonite = {
                import coursier.core.Version
                Version(maxAmmoniteScalaVer) < Version(defaultScalaVer)
              }
              ammonite.contains(true) &&
              ammoniteVersionOpt.isEmpty &&
              needsDowngradeForAmmonite
            }
            if (shouldDowngrade) {
              logger.message(
                s"Scala $defaultScalaVer is not yet supported with this version of Ammonite"
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
            sharedJava.allJavaOpts.map(JavaOpt(_)).map(Positioned.commandLine) ++
            (if (spark) Seq(Positioned.none(JavaOpt("-Dscala.usejavacp=true"))) else Nil),
        jvmIdOpt = baseOptions.javaOptions.jvmIdOpt
          .orElse(if (spark) Some(Positioned.none("8")) else None)
      ),
      notForBloopOptions = baseOptions.notForBloopOptions.copy(
        replOptions = baseOptions.notForBloopOptions.replOptions.copy(
          useAmmoniteOpt = ammonite,
          ammoniteVersionOpt = ammoniteVersion.map(_.trim).filter(_.nonEmpty),
          ammoniteArgs = ammoniteArg
        ),
        addRunnerDependencyOpt = baseOptions.notForBloopOptions.addRunnerDependencyOpt
          .orElse(Some(false))
      ),
      internal = baseOptions.internal.copy(
        keepResolution = baseOptions.internal.keepResolution || spark
      )
    )
  }

  private def runMode(options: ReplOptions): RunMode.HasRepl = {
    def sparkReplOptions =
      options.sharedRepl.predef.filter(_.trim.nonEmpty)
        .map(p => Seq("-I", p))
        .getOrElse(Nil)
    if (options.sharedRepl.standaloneSpark) RunMode.StandaloneSparkSubmit(Nil, sparkReplOptions)
    else if (options.sharedRepl.spark) RunMode.SparkSubmit(Nil, sparkReplOptions)
    else RunMode.Default
  }

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

    val compilerMaker = options.shared.compilerMaker(threads).orExit(logger)

    val directories = Directories.directories

    def buildFailed(allowExit: Boolean): Unit = {
      System.err.println("Compilation failed")
      if (allowExit)
        sys.exit(1)
    }
    def buildCancelled(allowExit: Boolean): Unit = {
      System.err.println("Build cancelled")
      if (allowExit)
        sys.exit(1)
    }

    def doRunRepl(
      buildOptions: BuildOptions,
      artifacts: Artifacts,
      classDir: Option[os.Path],
      allowExit: Boolean,
      runMode: RunMode.HasRepl,
      buildOpt: Option[Build.Successful]
    ): Unit = {
      val res = runRepl(
        buildOptions,
        programArgs,
        artifacts,
        classDir,
        directories,
        logger,
        allowExit = allowExit,
        options.sharedRepl.replDryRun,
        runMode,
        buildOpt
      )
      res match {
        case Left(ex) =>
          if (allowExit) logger.exit(ex)
          else logger.log(ex)
        case Right(()) =>
      }
    }
    def doRunReplFromBuild(
      build: Build.Successful,
      allowExit: Boolean,
      runMode: RunMode.HasRepl
    ): Unit =
      doRunRepl(
        build.options,
        build.artifacts,
        build.outputOpt,
        allowExit,
        runMode,
        Some(build)
      )

    val cross    = options.sharedRepl.compileCross.cross.getOrElse(false)
    val configDb = options.shared.configDb.orExit(logger)
    val actionableDiagnostics =
      options.shared.logging.verbosityOptions.actions.orElse(
        configDb.get(Keys.actions).getOrElse(None)
      )

    if (inputs.isEmpty) {
      val artifacts = initialBuildOptions.artifacts(logger, Scope.Main).orExit(logger)
      // synchronizing, so that multiple presses to enter (handled by WatchUtil.waitForCtrlC)
      // don't try to run repls in parallel
      val lock = new Object
      def runThing() = lock.synchronized {
        doRunRepl(
          initialBuildOptions,
          artifacts,
          None,
          allowExit = !options.sharedRepl.watch.watchMode,
          runMode = runMode(options),
          buildOpt = None
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
        buildTests = false,
        partial = None,
        actionableDiagnostics = actionableDiagnostics,
        postAction = () => WatchUtil.printWatchMessage()
      ) { res =>
        for (builds <- res.orReport(logger))
          builds.main match {
            case s: Build.Successful =>
              doRunReplFromBuild(s, allowExit = false, runMode = runMode(options))
            case _: Build.Failed    => buildFailed(allowExit = false)
            case _: Build.Cancelled => buildCancelled(allowExit = false)
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
          buildTests = false,
          partial = None,
          actionableDiagnostics = actionableDiagnostics
        )
          .orExit(logger)
      builds.main match {
        case s: Build.Successful =>
          doRunReplFromBuild(s, allowExit = true, runMode = runMode(options))
        case _: Build.Failed    => buildFailed(allowExit = true)
        case _: Build.Cancelled => buildCancelled(allowExit = true)
      }
    }
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
    artifacts: Artifacts,
    classDir: Option[os.Path],
    directories: scala.build.Directories,
    logger: Logger,
    allowExit: Boolean,
    dryRun: Boolean,
    runMode: RunMode.HasRepl,
    buildOpt: Option[Build.Successful]
  ): Either[BuildException, Unit] = either {

    val setupPython = options.notForBloopOptions.python.getOrElse(false)

    val cache             = options.internal.cache.getOrElse(FileCache())
    val shouldUseAmmonite = options.notForBloopOptions.replOptions.useAmmonite

    val scalaParams = artifacts.scalaOpt
      .getOrElse {
        sys.error("Expected Scala artifacts to be fetched")
      }
      .params

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
        val dirs = buildOpt.map(_.inputs.workspace).toSeq ++ Seq(os.pwd)
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

    def ammoniteAdditionalArgs(addAmmoniteSpark: Boolean = false) = {
      val pythonPredef =
        if (setupPython)
          """import me.shadaj.scalapy.py
            |import me.shadaj.scalapy.py.PyQuote
            |""".stripMargin
        else
          ""
      val (sparkArgs, sparkPredef) =
        if (addAmmoniteSpark) {
          val predef =
            s"""import $$ivy.`sh.almond::ammonite-spark:${scala.cli.internal.Constants.ammoniteSparkVersion}`
               |import org.apache.spark._
               |import org.apache.spark.sql._
               |
               |val spark = AmmoniteSparkSession.builder()(implicitly, ammonite.repl.ReplBridge.value0)
               |  .progressBars()
               |  .config(new SparkConf)
               |  .config("spark.master", Option(System.getenv("SPARK_MASTER")).orElse(sys.props.get("spark.master")).getOrElse("local[*]"))
               |  .getOrCreate()
               |def sc = spark.sparkContext
               |""".stripMargin
          (Seq("--class-based"), predef)
        }
        else
          (Nil, "")
      val pysparkPredef =
        if (setupPython && addAmmoniteSpark)
          """py.module("pyspark.context").SparkContext._ensure_initialized()
            |val gw = py.module("pyspark.context").SparkContext._gateway
            |py.module("pyspark.context").SparkContext(
            |  jsc = gw.jvm.JavaSparkContext(gw.jvm.org.apache.spark.SparkContext.getOrCreate()),
            |  conf = py.module("pyspark.conf").SparkConf(_jconf = gw.jvm.org.apache.spark.SparkContext.getOrCreate().getConf())
            |)
            |py.module("pyspark.sql").SparkSession._create_shell_session()
            |me.shadaj.scalapy.interpreter.CPythonInterpreter.execManyLines {
            |  new String(
            |    java.nio.file.Files.readAllBytes(
            |      java.nio.file.Paths.get(
            |        py.module("inspect")
            |          .getfile(py.module("pyspark.context").SparkContext)
            |          .as[String]
            |      )
            |        .getParent
            |        .resolve("python/pyspark/shell.py")
            |    )
            |  )
            |}
            |""".stripMargin
        else
          ""
      val predef =
        Seq(pythonPredef, sparkPredef, pysparkPredef).map(_.trim).filter(_.nonEmpty).mkString(
          System.lineSeparator()
        )
      val predefArgs =
        if (predef.trim.isEmpty) Nil
        else Seq("--predef-code", predef)
      sparkArgs ++ predefArgs ++ options.notForBloopOptions.replOptions.ammoniteArgs
    }

    // TODO Warn if some entries of artifacts.classPath were evicted in replArtifacts.replClassPath
    //      (should be artifacts whose version was bumped by Ammonite).

    // TODO Find the common namespace of all user classes, and import it all in the Ammonite session.

    // TODO Allow to disable printing the welcome banner and the "Loading..." message in Ammonite.

    val rootClasses = classDir
      .toSeq
      .flatMap(os.list(_))
      .filter(_.last.endsWith(".class"))
      .filter(os.isFile(_)) // just in case
      .map(_.last.stripSuffix(".class"))
      .sorted
    val warnRootClasses = rootClasses.nonEmpty &&
      options.notForBloopOptions.replOptions.useAmmoniteOpt.contains(true)
    if (warnRootClasses)
      logger.message(
        s"Warning: found classes defined in the root package (${rootClasses.mkString(", ")})." +
          " These will not be accessible from the REPL."
      )

    lazy val py4jGatewaySecret: String = {
      val random = new SecureRandom
      val secret = Array.ofDim[Byte](256 / 8)
      random.nextBytes(secret)
      new BigInteger(1, secret).toString(16)
    }
    lazy val py4jGatewayRandomPort: Int = {
      val server = new java.net.ServerSocket(0)
      try server.getLocalPort
      finally server.close()
    }
    def py4jEnvVars: Map[String, String] =
      Map(
        "PYSPARK_GATEWAY_SECRET" -> py4jGatewaySecret,
        "PYSPARK_GATEWAY_PORT"   -> py4jGatewayRandomPort.toString
      )
    def py4jProps: Map[String, String] =
      Map(
        "with-py4j.secret" -> py4jGatewaySecret,
        "with-py4j.port"   -> py4jGatewayRandomPort.toString
      )

    def actualBuild: Build.Successful =
      buildOpt.getOrElse {
        val ws      = os.temp.dir()
        val inputs  = Inputs.empty(ws, enableMarkdown = false)
        val sources = Sources(Nil, Nil, None, Nil, options)
        val scope   = Scope.Main
        Build.Successful(
          inputs = inputs,
          options = options,
          scalaParams = Some(scalaParams),
          scope = scope,
          sources = Sources(Nil, Nil, None, Nil, options),
          artifacts = artifacts,
          project = value(Build.buildProject(inputs, sources, Nil, options, None, scope, logger)),
          output = classDir.getOrElse(ws),
          diagnostics = None,
          generatedSources = Nil,
          isPartial = false
        )
      }

    def maybeRunRepl(
      replArtifacts: ReplArtifacts,
      replArgs: Seq[String],
      extraEnv: Map[String, String] = Map.empty,
      extraProps: Map[String, String] = Map.empty,
      withPy4j: Boolean = false
    ): Unit =
      if (dryRun)
        logger.message("Dry run, not running REPL.")
      else {
        val (actualMainClass, extraFirstArgs) =
          if (withPy4j)
            ("withpy4j.WithPy4j", Seq(replArtifacts.replMainClass))
          else
            (replArtifacts.replMainClass, Nil)
        val retCode = Runner.runJvm(
          options.javaHome().value.javaCommand,
          scalapyJavaOpts ++
            replArtifacts.replJavaOpts ++
            options.javaOptions.javaOpts.toSeq.map(_.value.value) ++
            extraProps.toVector.sorted.map { case (k, v) => s"-D$k=$v" },
          classDir.toSeq ++ replArtifacts.replClassPath,
          actualMainClass,
          maybeAdaptForWindows(extraFirstArgs ++ replArgs),
          logger,
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
          artifacts.userDependencies,
          artifacts.extraClassPath,
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
    def withPy4jArtifacts(): Either[BuildException, Seq[(String, os.Path)]] = either {
      val repositories = value(options.finalRepositories)
      value {
        ReplArtifacts.withPy4j(
          logger,
          cache,
          repositories
        )
      }
    }
    def ammoniteArtifacts(withPy4j: Boolean = false): Either[BuildException, ReplArtifacts] =
      ReplArtifacts.ammonite(
        scalaParams,
        options.notForBloopOptions.replOptions.ammoniteVersion,
        artifacts.userDependencies ++ Seq(dep"io.github.alexarchambault.py4j:with-py4j:0.1.1"),
        artifacts.extraClassPath,
        artifacts.extraSourceJars,
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

        case mode: RunMode.Spark =>
          mode match {
            case _: RunMode.SparkSubmit =>
              ???
            case _: RunMode.StandaloneSparkSubmit =>
              val replArtifacts = value(ammoniteArtifacts(withPy4j = setupPython))
              val replArgs      = ammoniteAdditionalArgs(addAmmoniteSpark = true) ++ programArgs
              maybeRunRepl(
                replArtifacts,
                replArgs,
                extraEnv = py4jEnvVars,
                extraProps = py4jProps,
                withPy4j = setupPython
              )
          }
      }
    else
      runMode match {
        case RunMode.Default =>
          val replArtifacts = value(defaultArtifacts())
          val replArgs      = additionalArgs ++ programArgs
          maybeRunRepl(replArtifacts, replArgs)

        case mode: RunMode.Spark =>
          val build = actualBuild
          // FIXME scalac options are ignored here
          val res = value {
            mode match {
              case _: RunMode.SparkSubmit =>
                RunSpark.run(
                  build,
                  "org.apache.spark.repl.Main",
                  additionalArgs ++ mode.replArgs,
                  programArgs,
                  logger,
                  allowExit,
                  dryRun,
                  None,
                  extraJavaOpts = scalapyJavaOpts ++ py4jProps.toVector.sorted.map { case (k, v) =>
                    s"-D$k=$v"
                  },
                  extraEnv = py4jEnvVars,
                  extraJars = value(withPy4jArtifacts()).map(_._2),
                  withPy4j = setupPython
                )
              case _: RunMode.StandaloneSparkSubmit =>
                RunSpark.runStandalone(
                  build,
                  "org.apache.spark.repl.Main",
                  additionalArgs ++ mode.replArgs,
                  programArgs,
                  logger,
                  allowExit,
                  dryRun,
                  None,
                  extraJavaOpts = scalapyJavaOpts ++ py4jProps.toVector.sorted.map { case (k, v) =>
                    s"-D$k=$v"
                  },
                  extraEnv = py4jEnvVars,
                  extraJars = value(withPy4jArtifacts()).map(_._2),
                  withPy4j = setupPython
                )
            }
          }
          if (dryRun)
            logger.message("Dry run, not running REPL.")
          else {
            val (proc, hookOpt) = res match {
              case Left(_) =>
                // can only be left if showCommand == true, that is dryRun == true
                sys.error("Cannot happen")
              case Right(r) => r
            }
            val retCode =
              try proc.waitFor()
              finally hookOpt.foreach(_())
            if (retCode != 0)
              value(Left(new ReplError(retCode)))
          }
      }
  }

  final class ReplError(retCode: Int)
      extends BuildException(s"Failed to run REPL (exit code: $retCode)")
}
