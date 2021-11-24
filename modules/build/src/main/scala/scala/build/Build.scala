package scala.build

import ch.epfl.scala.bsp4j
import com.swoval.files.FileTreeViews.Observer
import com.swoval.files.{PathWatcher, PathWatchers}
import dependency.ScalaParameters

import java.io.File
import java.nio.file.{FileSystemException, Path}
import java.util.concurrent.{ScheduledExecutorService, ScheduledFuture}

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
import scala.build.bloop.BloopServer
import scala.build.blooprifle.{BloopRifleConfig, VersionUtil}
import scala.build.errors._
import scala.build.internal.{Constants, CustomCodeWrapper, MainClass, Util}
import scala.build.options.{BuildOptions, ClassPathOptions, Platform, Scope}
import scala.build.postprocessing._
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt
import scala.util.Properties
import scala.util.control.NonFatal

trait Build {
  def inputs: Inputs
  def options: BuildOptions
  def scope: Scope
  def outputOpt: Option[os.Path]
  def success: Boolean
  def diagnostics: Option[Seq[(Either[String, os.Path], bsp4j.Diagnostic)]]

  def successfulOpt: Option[Build.Successful]
}

object Build {

  final case class Successful(
    inputs: Inputs,
    options: BuildOptions,
    scalaParams: ScalaParameters,
    scope: Scope,
    sources: Sources,
    artifacts: Artifacts,
    project: Project,
    output: os.Path,
    diagnostics: Option[Seq[(Either[String, os.Path], bsp4j.Diagnostic)]]
  ) extends Build {
    def success: Boolean               = true
    def successfulOpt: Some[this.type] = Some(this)
    def outputOpt: Some[os.Path]       = Some(output)
    def fullClassPath: Seq[Path] =
      Seq(output.toNIO) ++ sources.resourceDirs.map(_.toNIO) ++ artifacts.classPath
    def foundMainClasses(): Seq[String] =
      MainClass.find(output)
    def retainedMainClass: Either[MainClassError, String] = {
      lazy val foundMainClasses0 = foundMainClasses()
      val defaultMainClassOpt = sources.mainClass
        .filter(name => foundMainClasses0.contains(name))
      def foundMainClass =
        if (foundMainClasses0.isEmpty) {
          val msg = "No main class found"
          System.err.println(msg)
          Left(new NoMainClassFoundError)
        }
        else if (foundMainClasses0.length == 1) Right(foundMainClasses0.head)
        else
          Left(
            new SeveralMainClassesFoundError(
              ::(foundMainClasses0.head, foundMainClasses0.tail.toList)
            )
          )

      defaultMainClassOpt match {
        case Some(cls) => Right(cls)
        case None      => foundMainClass
      }
    }

    def crossKey: CrossKey =
      CrossKey(
        BuildOptions.CrossKey(
          scalaParams.scalaVersion,
          options.platform
        ),
        scope
      )
  }

  final case class Failed(
    inputs: Inputs,
    options: BuildOptions,
    scope: Scope,
    sources: Sources,
    artifacts: Artifacts,
    project: Project,
    diagnostics: Option[Seq[(Either[String, os.Path], bsp4j.Diagnostic)]]
  ) extends Build {
    def success: Boolean         = false
    def successfulOpt: None.type = None
    def outputOpt: None.type     = None
  }

  final case class Cancelled(
    inputs: Inputs,
    options: BuildOptions,
    scope: Scope,
    reason: String
  ) extends Build {
    def success: Boolean         = false
    def successfulOpt: None.type = None
    def outputOpt: None.type     = None
    def diagnostics: None.type   = None
  }

  def updateInputs(
    inputs: Inputs,
    options: BuildOptions
  ): Inputs = {

    // If some options are manually overridden, append a hash of the options to the project name
    // Using options, not options0 - only the command-line options are taken into account. No hash is
    // appended for options from the sources.
    val optionsHash = options.hash

    inputs.copy(
      baseProjectName = inputs.baseProjectName + optionsHash.map("_" + _).getOrElse("")
    )
  }

  private def build(
    inputs: Inputs,
    options: BuildOptions,
    logger: Logger,
    buildClient: BloopBuildClient,
    bloopServer: bloop.BloopServer,
    crossBuilds: Boolean
  ): Either[BuildException, Builds] = either {

    val crossSources = value {
      CrossSources.forInputs(
        inputs,
        Sources.defaultPreprocessors(options.scriptOptions.codeWrapper.getOrElse(CustomCodeWrapper))
      )
    }
    val sharedOptions = crossSources.sharedOptions(options)
    val crossOptions  = sharedOptions.crossOptions

    def doBuild(
      overrideOptions: BuildOptions,
      scope: Scope
    ): Either[BuildException, Build] = either {

      val inputs0 = updateInputs(
        inputs,
        overrideOptions.orElse(options) // update hash in inputs with options coming from the CLI or cross-building, not from the sources
      )

      val baseOptions = overrideOptions.orElse(sharedOptions)

      val crossSources0 = crossSources.withVirtualDir(inputs0, scope, baseOptions)

      val sources = value(crossSources0.scopedSources(baseOptions))
        .sources(scope, baseOptions)

      val generatedSources = sources.generateSources(inputs0.generatedSrcRoot(scope))
      val buildOptions     = sources.buildOptions

      val scopeParams =
        if (scope == Scope.Main) Nil
        else Seq(scope.name)
      buildClient.setProjectParams(scopeParams ++ value(buildOptions.projectParams))

      val res = build(
        inputs0,
        sources,
        inputs0.generatedSrcRoot(scope),
        generatedSources,
        buildOptions,
        scope,
        logger,
        buildClient,
        bloopServer
      )
      value(res)
    }

    def buildScope(
      scope: Scope,
      parentBuildOpt: Option[Build],
      parentExtraBuildsOpt: Option[Seq[Build]]
    ): Either[BuildException, (Build, Seq[Build])] =
      either {
        val mainBuild = value {
          parentBuildOpt match {
            case None => doBuild(BuildOptions(), scope)
            case Some(s: Build.Successful) =>
              val extraOptions = BuildOptions(
                classPathOptions = ClassPathOptions(
                  extraClassPath = Seq(s.output)
                )
              )
              doBuild(extraOptions, scope)
            case Some(_) =>
              Right(Build.Cancelled(
                inputs,
                sharedOptions,
                scope,
                "Parent build failed or cancelled"
              ))
          }
        }

        val extraBuilds =
          if (crossBuilds)
            value {
              val maybeBuilds = parentExtraBuildsOpt match {
                case None =>
                  crossOptions.map { opt =>
                    doBuild(opt, scope)
                  }
                case Some(parentExtraBuilds) =>
                  crossOptions.zip(parentExtraBuilds)
                    .map {
                      case (opt, parentBuildOpt) =>
                        parentBuildOpt match {
                          case s: Build.Successful =>
                            val updatedOptions = opt.copy(
                              classPathOptions = sharedOptions.classPathOptions.copy(
                                extraClassPath =
                                  sharedOptions.classPathOptions.extraClassPath :+ s.output
                              )
                            )
                            doBuild(updatedOptions, scope)
                          case _ =>
                            Right(Build.Cancelled(
                              inputs,
                              opt,
                              scope,
                              "Parent build failed or cancelled"
                            ))
                        }
                    }
              }
              maybeBuilds
                .sequence
                .left.map(CompositeBuildException(_))
            }
          else
            Nil

        (mainBuild, extraBuilds)
      }

    val (mainBuild, extraBuilds) = value(buildScope(Scope.Main, None, None))
    val (testBuild, extraTestBuilds) =
      value(buildScope(Scope.Test, Some(mainBuild), Some(extraBuilds)))

    Builds(Seq(mainBuild, testBuild), Seq(extraBuilds, extraTestBuilds))
  }

  private def build(
    inputs: Inputs,
    sources: Sources,
    generatedSrcRoot0: os.Path,
    generatedSources: Seq[GeneratedSource],
    options: BuildOptions,
    scope: Scope,
    logger: Logger,
    buildClient: BloopBuildClient,
    bloopServer: bloop.BloopServer
  ): Either[BuildException, Build] = either {

    val build0 = value {
      buildOnce(
        inputs,
        sources,
        generatedSrcRoot0,
        generatedSources,
        options,
        scope,
        logger,
        buildClient,
        bloopServer
      )
    }

    build0 match {
      case successful: Successful =>
        if (options.jmhOptions.runJmh.getOrElse(false) && scope == Scope.Main)
          value {
            val res = jmhBuild(
              inputs,
              successful,
              logger,
              successful.options.javaHome().javaCommand,
              buildClient,
              bloopServer
            )
            res.flatMap {
              case Some(b) => Right(b)
              case None    => Left(new JmhBuildFailedError)
            }
          }
        else
          build0
      case _ => build0
    }
  }

  def classesRootDir(root: os.Path, projectName: String): os.Path =
    root / ".scala" / projectName / "classes"
  def classesDir(root: os.Path, projectName: String, scope: Scope): os.Path =
    classesRootDir(root, projectName) / scope.name

  def scalaNativeSupported(options: BuildOptions, inputs: Inputs) = either {
    val version = value(options.scalaParams).scalaVersion
    if (version.startsWith("3")) false
    else if (version.startsWith("2.13")) Properties.isMac || Properties.isLinux
    else if (version.startsWith("2.12"))
      !inputs.sourceFiles().exists {
        case _: Inputs.AnyScript => true
        case _                   => false
      }
    else false
  }

  def build(
    inputs: Inputs,
    options: BuildOptions,
    threads: BuildThreads,
    bloopConfig: BloopRifleConfig,
    logger: Logger,
    crossBuilds: Boolean
  ): Either[BuildException, Builds] = {
    val buildClient = BloopBuildClient.create(
      logger,
      keepDiagnostics = options.internal.keepDiagnostics
    )
    val classesDir0 = classesRootDir(inputs.workspace, inputs.projectName)

    bloop.BloopServer.withBuildServer(
      bloopConfig,
      "scala-cli",
      Constants.version,
      inputs.workspace.toNIO,
      classesDir0.toNIO,
      buildClient,
      threads.bloop,
      logger.bloopRifleLogger
    ) { bloopServer =>
      build(
        inputs = inputs,
        options = options,
        logger = logger,
        buildClient = buildClient,
        bloopServer = bloopServer,
        crossBuilds = crossBuilds
      )
    }
  }

  def build(
    inputs: Inputs,
    options: BuildOptions,
    bloopConfig: BloopRifleConfig,
    logger: Logger,
    crossBuilds: Boolean
  ): Either[BuildException, Builds] =
    build(
      inputs,
      options, /*scope,*/ BuildThreads.create(),
      bloopConfig,
      logger,
      crossBuilds = crossBuilds
    )

  def watch(
    inputs: Inputs,
    options: BuildOptions,
    bloopConfig: BloopRifleConfig,
    logger: Logger,
    crossBuilds: Boolean,
    postAction: () => Unit = () => ()
  )(action: Either[BuildException, Builds] => Unit): Watcher = {

    val buildClient = BloopBuildClient.create(
      logger,
      keepDiagnostics = options.internal.keepDiagnostics
    )
    val threads     = BuildThreads.create()
    val classesDir0 = classesRootDir(inputs.workspace, inputs.projectName)
    val bloopServer = bloop.BloopServer.buildServer(
      bloopConfig,
      "scala-cli",
      Constants.version,
      inputs.workspace.toNIO,
      classesDir0.toNIO,
      buildClient,
      threads.bloop,
      logger.bloopRifleLogger
    )

    def run() = {
      try {
        val res = build(
          inputs,
          options,
          logger,
          buildClient,
          bloopServer,
          crossBuilds = crossBuilds
        )
        action(res)
      }
      catch {
        case NonFatal(e) =>
          Util.printException(e)
      }
      postAction()
    }

    run()

    val watcher = new Watcher(ListBuffer(), threads.fileWatcher, run(), bloopServer.shutdown())

    def doWatch(): Unit =
      for (elem <- inputs.elements) {
        val depth = elem match {
          case _: Inputs.SingleFile => -1
          case _                    => Int.MaxValue
        }
        val eventFilter: PathWatchers.Event => Boolean = elem match {
          case d: Inputs.Directory =>
            // Filtering event for directories, to ignore those related to the .bloop directory in particular
            event =>
              val p           = os.Path(event.getTypedPath.getPath.toAbsolutePath)
              val relPath     = p.relativeTo(d.path)
              val isHidden    = relPath.segments.exists(_.startsWith("."))
              def isScalaFile = relPath.last.endsWith(".sc") || relPath.last.endsWith(".scala")
              def isJavaFile  = relPath.last.endsWith(".java")
              !isHidden && (isScalaFile || isJavaFile)
          case _ => _ => true
        }

        val watcher0 = watcher.newWatcher()
        elem match {
          case d: Inputs.OnDisk =>
            watcher0.register(d.path.toNIO, depth)
          case _: Inputs.Virtual =>
        }
        watcher0.addObserver {
          onChangeBufferedObserver { event =>
            if (eventFilter(event))
              watcher.schedule()
          }
        }
      }

    try doWatch()
    catch {
      case NonFatal(e) =>
        watcher.dispose()
        throw e
    }

    watcher
  }

  def prepareBuild(
    inputs: Inputs,
    sources: Sources,
    generatedSources: Seq[GeneratedSource],
    options: BuildOptions,
    scope: Scope,
    logger: Logger,
    buildClient: BloopBuildClient,
    bloopServer: Option[BloopServer]
  ): Either[BuildException, (os.Path, ScalaParameters, Artifacts, Project, Boolean)] = either {

    val params     = value(options.scalaParams)
    val allSources = sources.paths.map(_._1) ++ generatedSources.map(_.generated)

    val classesDir0 = classesDir(inputs.workspace, inputs.projectName, scope)

    val artifacts = value(options.artifacts(logger))

    val pluginScalacOptions = artifacts.compilerPlugins.distinct.map {
      case (_, _, path) =>
        s"-Xplugin:${path.toAbsolutePath}"
    }

    val semanticDbScalacOptions =
      if (options.scalaOptions.generateSemanticDbs.getOrElse(false))
        if (params.scalaVersion.startsWith("2."))
          Seq(
            "-Yrangepos",
            "-P:semanticdb:failures:warning",
            "-P:semanticdb:synthetics:on",
            s"-P:semanticdb:sourceroot:${inputs.workspace}"
          )
        else
          Seq(
            "-Xsemanticdb"
          )
      else Nil

    val sourceRootScalacOptions =
      if (params.scalaVersion.startsWith("2.")) Nil
      else Seq("-sourceroot", inputs.workspace.toString)

    val scalaJsScalacOptions =
      if (options.platform == Platform.JS && !params.scalaVersion.startsWith("2.")) Seq("-scalajs")
      else Nil

    val bloopJvmRelease = for {
      bloopServer0 <- bloopServer
      version      <- VersionUtil.jvmRelease(bloopServer0.jvmVersion)
    } yield version
    val javaV            = options.javaHome().version.toString
    val isReleaseFlagSet = options.scalaOptions.scalacOptions.contains("-release")
    val scalacReleaseV =
      if (bloopJvmRelease.contains(8) || isReleaseFlagSet) Nil else List("-release", javaV)
    val javacReleaseV =
      if (bloopJvmRelease.contains(8) || isReleaseFlagSet) Nil else List("--release", javaV)

    val scalacOptions = options.scalaOptions.scalacOptions ++
      pluginScalacOptions ++
      semanticDbScalacOptions ++
      sourceRootScalacOptions ++
      scalaJsScalacOptions ++ scalacReleaseV

    val scalaCompiler = ScalaCompiler(
      scalaVersion = params.scalaVersion,
      scalaBinaryVersion = params.scalaBinaryVersion,
      scalacOptions = scalacOptions,
      compilerClassPath = artifacts.compilerClassPath
    )

    // `test` scope should contains class path to main scope
    val mainClassesPath =
      if (scope == Scope.Test)
        List(classesDir(inputs.workspace, inputs.projectName, Scope.Main).toNIO)
      else Nil

    val project = Project(
      workspace = inputs.workspace / ".scala",
      classesDir = classesDir0,
      scalaCompiler = scalaCompiler,
      scalaJsOptions =
        if (options.platform == Platform.JS) Some(options.scalaJsOptions.config)
        else None,
      scalaNativeOptions =
        if (options.platform == Platform.Native) Some(options.scalaNativeOptions.bloopConfig())
        else None,
      projectName = inputs.scopeProjectName(scope),
      classPath = artifacts.compileClassPath ++ mainClassesPath,
      resolution = Some(Project.resolution(artifacts.detailedArtifacts)),
      sources = allSources,
      resourceDirs = sources.resourceDirs,
      scope = scope,
      javaHomeOpt = Option(options.javaHomeLocation()),
      javacOptions = javacReleaseV
    )

    val updatedBloopConfig = project.writeBloopFile(logger)

    if (updatedBloopConfig && os.isDir(classesDir0)) {
      logger.debug(s"Clearing $classesDir0")
      os.list(classesDir0).foreach { p =>
        logger.debug(s"Removing $p")
        try os.remove.all(p)
        catch {
          case ex: FileSystemException =>
            logger.debug(s"Ignoring $ex while cleaning up $p")
        }
      }
    }

    buildClient.clear()
    buildClient.setGeneratedSources(generatedSources)

    (classesDir0, params, artifacts, project, updatedBloopConfig)
  }

  def buildOnce(
    inputs: Inputs,
    sources: Sources,
    generatedSrcRoot0: os.Path,
    generatedSources: Seq[GeneratedSource],
    options: BuildOptions,
    scope: Scope,
    logger: Logger,
    buildClient: BloopBuildClient,
    bloopServer: bloop.BloopServer
  ): Either[BuildException, Build] = either {
    if (options.platform == Platform.Native && !value(scalaNativeSupported(options, inputs)))
      value(Left(new ScalaNativeCompatibilityError()))
    else
      value(Right(0))

    val (classesDir0, scalaParams, artifacts, project, updatedBloopConfig) = value {
      prepareBuild(
        inputs,
        sources,
        generatedSources,
        options,
        scope,
        logger,
        buildClient,
        Some(bloopServer)
      )
    }

    if (updatedBloopConfig && os.isDir(classesDir0)) {
      logger.debug(s"Clearing $classesDir0")
      os.list(classesDir0).foreach { p =>
        logger.debug(s"Removing $p")
        try os.remove.all(p)
        catch {
          case ex: FileSystemException =>
            logger.debug(s"Ignore $ex while removing $p")
        }
      }
    }

    buildClient.clear()
    buildClient.setGeneratedSources(generatedSources)
    val success = Bloop.compile(
      inputs.scopeProjectName(scope),
      bloopServer,
      logger,
      buildTargetsTimeout = 20.seconds
    )

    if (success) {
      postProcess(
        generatedSources,
        generatedSrcRoot0,
        classesDir0,
        logger,
        inputs.workspace,
        updateSemanticDbs = true,
        scalaVersion = project.scalaCompiler.scalaVersion
      )
        .left.foreach(_.foreach(logger.message(_)))

      Successful(
        inputs,
        options,
        scalaParams,
        scope,
        sources,
        artifacts,
        project,
        classesDir0,
        buildClient.diagnostics
      )
    }
    else
      Failed(
        inputs,
        options,
        scope,
        sources,
        artifacts,
        project,
        buildClient.diagnostics
      )
  }

  def postProcess(
    generatedSources: Seq[GeneratedSource],
    generatedSrcRoot: os.Path,
    classesDir: os.Path,
    logger: Logger,
    workspace: os.Path,
    updateSemanticDbs: Boolean,
    scalaVersion: String
  ): Either[Seq[String], Unit] = {

    // TODO Write classes to a separate directory during post-processing
    logger.debug("Post-processing class files of pre-processed sources")
    val mappings = generatedSources
      .map { source =>
        val lineShift =
          -os.read(source.generated).take(source.topWrapperLen).count(_ == '\n') // charset?
        val relPath       = source.generated.relativeTo(generatedSrcRoot).toString
        val reportingPath = source.reportingPath.fold(s => s, _.last)
        (relPath, (reportingPath, lineShift))
      }
      .toMap

    val postProcessors =
      Seq(ByteCodePostProcessor) ++
        (if (updateSemanticDbs) Seq(SemanticDbPostProcessor) else Nil) ++
        Seq(TastyPostProcessor)

    val failures = postProcessors.flatMap(
      _.postProcess(generatedSources, mappings, workspace, classesDir, logger, scalaVersion)
        .fold(e => Seq(e), _ => Nil)
    )
    if (failures.isEmpty) Right(()) else Left(failures)
  }

  def onChangeBufferedObserver(onEvent: PathWatchers.Event => Unit): Observer[PathWatchers.Event] =
    new Observer[PathWatchers.Event] {
      def onError(t: Throwable): Unit = {
        // TODO Log that properly
        System.err.println("got error:")
        def printEx(t: Throwable): Unit =
          if (t != null) {
            System.err.println(t)
            System.err.println(
              t.getStackTrace.iterator.map("  " + _ + System.lineSeparator()).mkString
            )
            printEx(t.getCause)
          }
        printEx(t)
      }

      def onNext(event: PathWatchers.Event): Unit =
        onEvent(event)
    }

  final class Watcher(
    val watchers: ListBuffer[PathWatcher[PathWatchers.Event]],
    val scheduler: ScheduledExecutorService,
    onChange: => Unit,
    onDispose: => Unit
  ) {
    def newWatcher(): PathWatcher[PathWatchers.Event] = {
      val w = PathWatchers.get(true)
      watchers += w
      w
    }
    def dispose(): Unit = {
      onDispose
      watchers.foreach(_.close())
      scheduler.shutdown()
    }

    private val lock                  = new Object
    private var f: ScheduledFuture[_] = null
    private val waitFor               = 50.millis
    private val runnable: Runnable = { () =>
      lock.synchronized {
        f = null
      }
      onChange // FIXME Log exceptions
    }
    def schedule(): Unit =
      if (f == null)
        lock.synchronized {
          if (f == null)
            f = scheduler.schedule(runnable, waitFor.length, waitFor.unit)
        }
  }

  private def printable(path: os.Path): String =
    if (path.startsWith(os.pwd)) path.relativeTo(os.pwd).toString
    else path.toString

  private def jmhBuild(
    inputs: Inputs,
    build: Build.Successful,
    logger: Logger,
    javaCommand: String,
    buildClient: BloopBuildClient,
    bloopServer: bloop.BloopServer
  ): Either[BuildException, Option[Build]] = either {
    val jmhProjectName = inputs.projectName + "_jmh"
    val jmhOutputDir   = inputs.workspace / ".scala" / jmhProjectName
    os.remove.all(jmhOutputDir)
    val jmhSourceDir   = jmhOutputDir / "sources"
    val jmhResourceDir = jmhOutputDir / "resources"

    val retCode = run(
      javaCommand,
      build.fullClassPath.map(_.toFile),
      "org.openjdk.jmh.generators.bytecode.JmhBytecodeGenerator",
      Seq(printable(build.output), printable(jmhSourceDir), printable(jmhResourceDir), "default"),
      logger
    )
    if (retCode != 0) {
      val red      = Console.RED
      val lightRed = "\u001b[91m"
      val reset    = Console.RESET
      System.err.println(
        s"${red}jmh bytecode generator exited with return code $lightRed$retCode$red.$reset"
      )
    }

    if (retCode == 0) {
      val jmhInputs = inputs.copy(
        baseProjectName = jmhProjectName,
        // hash of the underlying project if needed is already in jmhProjectName
        mayAppendHash = false,
        elements = inputs.elements ++ Seq(
          Inputs.Directory(jmhSourceDir),
          Inputs.ResourceDirectory(jmhResourceDir)
        )
      )
      val updatedOptions = build.options.copy(
        jmhOptions = build.options.jmhOptions.copy(
          runJmh = build.options.jmhOptions.runJmh.map(_ => false)
        )
      )
      val jmhBuilds = value {
        Build.build(
          jmhInputs,
          updatedOptions,
          logger,
          buildClient,
          bloopServer,
          crossBuilds = false
        )
      }
      Some(jmhBuilds.main)
    }
    else None
  }

  private def run(
    javaCommand: String,
    classPath: Seq[File],
    mainClass: String,
    args: Seq[String],
    logger: Logger
  ): Int = {

    val command =
      Seq(javaCommand) ++
        Seq(
          "-cp",
          classPath.iterator.map(_.getAbsolutePath).mkString(File.pathSeparator),
          mainClass
        ) ++
        args

    logger.log(
      s"Running ${command.mkString(" ")}",
      "  Running" + System.lineSeparator() +
        command.iterator.map(_ + System.lineSeparator()).mkString
    )

    new ProcessBuilder(command: _*)
      .inheritIO()
      .start()
      .waitFor()
  }
}
