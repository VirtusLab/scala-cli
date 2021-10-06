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
import scala.build.blooprifle.BloopRifleConfig
import scala.build.errors.{
  BuildException,
  CompositeBuildException,
  JmhBuildFailedError,
  MainClassError,
  NoMainClassFoundError,
  SeveralMainClassesFoundError
}
import scala.build.internal.{Constants, CustomCodeWrapper, MainClass, Util}
import scala.build.options.BuildOptions
import scala.build.postprocessing._
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt
import scala.util.control.NonFatal

trait Build {
  def inputs: Inputs
  def options: BuildOptions
  def sources: Sources
  def artifacts: Artifacts
  def project: Project
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
  }

  final case class Failed(
    inputs: Inputs,
    options: BuildOptions,
    sources: Sources,
    artifacts: Artifacts,
    project: Project,
    diagnostics: Option[Seq[(Either[String, os.Path], bsp4j.Diagnostic)]]
  ) extends Build {
    def success: Boolean         = false
    def successfulOpt: None.type = None
    def outputOpt: None.type     = None
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
  ): Either[BuildException, (Build, Seq[Build])] = either {

    val crossSources = value {
      CrossSources.forInputs(
        inputs,
        Sources.defaultPreprocessors(options.scriptOptions.codeWrapper.getOrElse(CustomCodeWrapper))
      )
    }

    val sources = value(crossSources.sources(options))

    val options0 = sources.buildOptions

    def doBuild(buildOptions: BuildOptions) = either {

      val inputs0 = updateInputs(
        inputs,
        options // update hash in inputs with options coming from the CLI, not from the sources
      )

      val generatedSources = sources.generateSources(inputs0.generatedSrcRoot)
      buildClient.setProjectParams(value(buildOptions.projectParams))

      val res = build(
        inputs0,
        sources,
        inputs0.generatedSrcRoot,
        generatedSources,
        buildOptions,
        logger,
        buildClient,
        bloopServer
      )
      value(res)
    }

    val mainBuild = value(doBuild(options0))

    val extraBuilds =
      if (crossBuilds)
        value {
          options0.crossOptions.map(opt => doBuild(opt))
            .sequence
            .left.map(CompositeBuildException(_))
        }
      else
        Nil

    (mainBuild, extraBuilds)
  }

  private def build(
    inputs: Inputs,
    sources: Sources,
    generatedSrcRoot0: os.Path,
    generatedSources: Seq[GeneratedSource],
    options: BuildOptions,
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
        logger,
        buildClient,
        bloopServer
      )
    }

    build0 match {
      case successful: Successful =>
        if (options.jmhOptions.runJmh.getOrElse(false))
          value {
            val res = jmhBuild(
              inputs,
              successful,
              logger,
              successful.options.javaCommand(),
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

  def classesDir(root: os.Path, projectName: String): os.Path =
    root / ".scala" / projectName / "classes"

  def build(
    inputs: Inputs,
    options: BuildOptions,
    threads: BuildThreads,
    bloopConfig: BloopRifleConfig,
    logger: Logger,
    crossBuilds: Boolean
  ): Either[BuildException, (Build, Seq[Build])] = {

    val buildClient = BloopBuildClient.create(
      logger,
      keepDiagnostics = options.internal.keepDiagnostics
    )
    val classesDir0 = classesDir(inputs.workspace, inputs.projectName)
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
  ): Either[BuildException, (Build, Seq[Build])] =
    build(inputs, options, BuildThreads.create(), bloopConfig, logger, crossBuilds = crossBuilds)

  def watch(
    inputs: Inputs,
    options: BuildOptions,
    bloopConfig: BloopRifleConfig,
    logger: Logger,
    crossBuilds: Boolean,
    postAction: () => Unit = () => ()
  )(action: Either[BuildException, (Build, Seq[Build])] => Unit): Watcher = {

    val buildClient = BloopBuildClient.create(
      logger,
      keepDiagnostics = options.internal.keepDiagnostics
    )
    val threads     = BuildThreads.create()
    val classesDir0 = classesDir(inputs.workspace, inputs.projectName)
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
    logger: Logger,
    buildClient: BloopBuildClient
  ): Either[BuildException, (os.Path, ScalaParameters, Artifacts, Project, Boolean)] = either {

    val params     = value(options.scalaParams)
    val allSources = sources.paths.map(_._1) ++ generatedSources.map(_.generated)

    val classesDir0 = classesDir(inputs.workspace, inputs.projectName)

    val artifacts = value(options.artifacts(logger))

    val pluginScalacOptions = artifacts.compilerPlugins.map {
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
      if (options.scalaJsOptions.enable && !params.scalaVersion.startsWith("2.")) Seq("-scalajs")
      else Nil

    val scalacOptions = options.scalaOptions.scalacOptions ++
      pluginScalacOptions ++
      semanticDbScalacOptions ++
      sourceRootScalacOptions ++
      scalaJsScalacOptions

    val scalaCompiler = ScalaCompiler(
      scalaVersion = params.scalaVersion,
      scalaBinaryVersion = params.scalaBinaryVersion,
      scalacOptions = scalacOptions,
      compilerClassPath = artifacts.compilerClassPath
    )

    val project = Project(
      workspace = inputs.workspace / ".scala",
      classesDir = classesDir0,
      scalaCompiler = scalaCompiler,
      scalaJsOptions = options.scalaJsOptions.config,
      scalaNativeOptions = options.scalaNativeOptions.bloopConfig,
      projectName = inputs.projectName,
      classPath = artifacts.compileClassPath,
      resolution = Some(Project.resolution(artifacts.detailedArtifacts)),
      sources = allSources,
      resourceDirs = sources.resourceDirs,
      javaHomeOpt = options.javaHomeLocationOpt()
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
    logger: Logger,
    buildClient: BloopBuildClient,
    bloopServer: bloop.BloopServer
  ): Either[BuildException, Build] = either {

    val (classesDir0, scalaParams, artifacts, project, updatedBloopConfig) = value {
      prepareBuild(
        inputs,
        sources,
        generatedSources,
        options,
        logger,
        buildClient
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
      inputs.projectName,
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
        updateTasty = project.scalaCompiler.scalaVersion.startsWith("3.")
      )

      Successful(
        inputs,
        options,
        scalaParams,
        sources,
        artifacts,
        project,
        classesDir0,
        buildClient.diagnostics
      )
    }
    else
      Failed(inputs, options, sources, artifacts, project, buildClient.diagnostics)
  }

  def postProcess(
    generatedSources: Seq[GeneratedSource],
    generatedSrcRoot: os.Path,
    classesDir: os.Path,
    logger: Logger,
    workspace: os.Path,
    updateSemanticDbs: Boolean,
    updateTasty: Boolean
  ): Unit = {

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
        (if (updateTasty) Seq(TastyPostProcessor) else Nil)

    for (p <- postProcessors)
      p.postProcess(generatedSources, mappings, workspace, classesDir, logger)
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
  ) = either {
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
      val (jmhBuild, _) = value {
        Build.build(
          jmhInputs,
          updatedOptions,
          logger,
          buildClient,
          bloopServer,
          crossBuilds = false
        )
      }
      Some(jmhBuild)
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
