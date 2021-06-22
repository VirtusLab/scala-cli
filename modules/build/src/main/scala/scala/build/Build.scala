package scala.build

import _root_.bloop.config.{Config => BloopConfig}
import ch.epfl.scala.bsp4j
import com.swoval.files.FileTreeViews.Observer
import com.swoval.files.{FileTreeRepositories, PathWatcher, PathWatchers}
import dependency._
import scala.build.blooprifle.BloopRifleConfig
import scala.build.internal.{AsmPositionUpdater, Constants, CustomCodeWrapper, LineConversion, MainClass, SemanticdbProcessor, Util}
import scala.build.options.BuildOptions
import scala.build.tastylib.TastyData

import java.io.{File, IOException}
import java.lang.{Boolean => JBoolean}
import java.nio.file.{Path, Paths}
import java.util.concurrent.{ExecutorService, ScheduledExecutorService, ScheduledFuture}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.scalanative.{build => sn}
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
    sources: Sources,
    artifacts: Artifacts,
    project: Project,
    output: os.Path,
    diagnostics: Option[Seq[(Either[String, os.Path], bsp4j.Diagnostic)]]
  ) extends Build {
    def success: Boolean = true
    def successfulOpt: Some[this.type] = Some(this)
    def outputOpt: Some[os.Path] = Some(output)
    def fullClassPath: Seq[Path] =
      Seq(output.toNIO) ++ sources.resourceDirs.map(_.toNIO) ++ artifacts.classPath
    def foundMainClasses(): Seq[String] =
      MainClass.find(output)
    def retainedMainClassOpt(warnIfSeveral: Boolean = false): Option[String] = {
      lazy val foundMainClasses0 = foundMainClasses()
      val defaultMainClassOpt = sources.mainClass
        .filter(name => foundMainClasses0.contains(name))
      def foundMainClassOpt =
        if (foundMainClasses0.isEmpty) {
          val msg = "No main class found"
          System.err.println(msg)
          sys.error(msg)
        }
        else if (foundMainClasses0.length == 1) foundMainClasses0.headOption
        else {
          if (warnIfSeveral) {
            System.err.println("Found several main classes:")
            for (name <- foundMainClasses0)
              System.err.println(s"  $name")
            System.err.println("Please specify which one to use with --main-class")
          }
          None
        }

      defaultMainClassOpt.orElse(foundMainClassOpt)
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
    def success: Boolean = false
    def successfulOpt: None.type = None
    def outputOpt: None.type = None
  }

  private def build(
    inputs: Inputs,
    options: BuildOptions,
    threads: BuildThreads,
    logger: Logger,
    cwd: os.Path,
    buildClient: BloopBuildClient,
    bloopServer: bloop.BloopServer
  ): Build = {

    val sources = Sources.forInputs(
      inputs,
      options.scriptOptions.codeWrapper.getOrElse(CustomCodeWrapper)
    )

    val options0 = options.orElse(sources.buildOptions)
    // If some options are manually overridden, append a hash of the options to the project name
    // Using options, not options0 - only the command-line options are taken into account. No hash is
    // appended for options from the sources.
    val optionsHash = options.hash

    val inputs0 = inputs.copy(
      baseProjectName = inputs.baseProjectName + optionsHash.map("_" + _).getOrElse("")
    )

    val params = options0.scalaParams

    val build0 = buildOnce(cwd, inputs0, params, sources, options0, threads, logger, buildClient, bloopServer)

    build0 match {
      case successful: Successful =>
        if (options0.jmhOptions.runJmh.getOrElse(false))
          jmhBuild(inputs0, successful, threads, logger, cwd, successful.options.javaCommand(), buildClient, bloopServer).getOrElse {
            sys.error("JMH build failed") // suppress stack trace?
          }
        else
          build0
      case _ => build0
    }
  }

  private def classesDir(root: os.Path, projectName: String): os.Path =
    root / ".scala" / projectName / "classes"

  private def generatedSrcRoot(root: os.Path, projectName: String) =
    defaultGeneratedSrcRoot(root, projectName)
  private def defaultGeneratedSrcRoot(root: os.Path, projectName: String) =
    root / ".scala" / projectName / "src_generated"

  def build(
    inputs: Inputs,
    options: BuildOptions,
    threads: BuildThreads,
    bloopConfig: BloopRifleConfig,
    logger: Logger,
    cwd: os.Path
  ): Build = {

    val buildClient = new BloopBuildClient(
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
        inputs,
        options,
        threads,
        logger,
        cwd,
        buildClient,
        bloopServer
      )
    }
  }

  def build(
    inputs: Inputs,
    options: BuildOptions,
    bloopConfig: BloopRifleConfig,
    logger: Logger,
    cwd: os.Path
  ): Build =
    build(inputs, options, BuildThreads.create(), bloopConfig, logger, cwd)

  def watch(
    inputs: Inputs,
    options: BuildOptions,
    bloopConfig: BloopRifleConfig,
    logger: Logger,
    cwd: os.Path,
    postAction: () => Unit = () => ()
  )(action: Build => Unit): Watcher = {

    val buildClient = new BloopBuildClient(
      logger,
      keepDiagnostics = options.internal.keepDiagnostics
    )
    val threads = BuildThreads.create()
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
        val build0 = build(inputs, options, threads, logger, cwd, buildClient, bloopServer)
        action(build0)
      } catch {
        case NonFatal(e) =>
          Util.printException(e)
      }
      postAction()
    }

    run()

    val watcher = new Watcher(ListBuffer(), threads.fileWatcher, run(), bloopServer.shutdown())

    try {
      for (elem <- inputs.elements) {
        val depth = elem match {
          case s: Inputs.SingleFile => -1
          case _ => Int.MaxValue
        }
        val eventFilter: PathWatchers.Event => Boolean = elem match {
          case d: Inputs.Directory =>
            // Filtering event for directories, to ignore those related to the .bloop directory in particular
            event =>
              val p = os.Path(event.getTypedPath.getPath.toAbsolutePath)
              val relPath = p.relativeTo(d.path)
              val isHidden = relPath.segments.exists(_.startsWith("."))
              def isScalaFile = relPath.last.endsWith(".sc") || relPath.last.endsWith(".scala")
              def isJavaFile = relPath.last.endsWith(".java")
              def isConfFile = relPath.last == "scala.conf" || relPath.last.endsWith(".scala.conf")
              !isHidden && (isScalaFile || isJavaFile || isConfFile)
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
    } catch {
      case NonFatal(e) =>
        watcher.dispose()
        throw e
    }

    watcher
  }

  private def buildOnce(
    sourceRoot: os.Path,
    inputs: Inputs,
    params: ScalaParameters,
    sources: Sources,
    options: BuildOptions,
    threads: BuildThreads,
    logger: Logger,
    buildClient: BloopBuildClient,
    bloopServer: bloop.BloopServer
  ): Build = {

    val generatedSrcRoot0 = generatedSrcRoot(inputs.workspace, inputs.projectName)
    val generatedSources = sources.generateSources(generatedSrcRoot0)
    val allSources = sources.paths.map(_._1) ++ generatedSources.map(_._1)

    val classesDir0 = classesDir(inputs.workspace, inputs.projectName)

    val artifacts = options.artifacts(params, sources.dependencies, logger)

    val pluginScalacOptions = artifacts.compilerPlugins.map {
      case (_, _, path) =>
        s"-Xplugin:${path.toAbsolutePath}"
    }

    val semanticDbScalacOptions =
      if (options.scalaOptions.generateSemanticDbs.getOrElse(false)) {
        if (params.scalaVersion.startsWith("2."))
          Seq(
            "-Yrangepos",
            "-P:semanticdb:failures:warning",
            "-P:semanticdb:synthetics:on",
            s"-P:semanticdb:sourceroot:${inputs.workspace}",
            s"-P:semanticdb:targetroot:${classesDir0}"
          )
        else
          Seq(
            "-Xsemanticdb",
            "-semanticdb-target", classesDir0.toString
          )
      }
      else Nil

    val sourceRootScalacOptions =
      if (params.scalaVersion.startsWith("2.")) Nil
      else Seq("-sourceroot", inputs.workspace.toString)

    val scalacOptions = options.scalaOptions.scalacOptions ++
      pluginScalacOptions ++
      semanticDbScalacOptions ++
      sourceRootScalacOptions

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
               classPath = artifacts.classPath,
                 sources = allSources,
            resourceDirs = sources.resourceDirs
    )

    if (project.writeBloopFile(logger) && os.isDir(classesDir0)) {
      logger.debug(s"Clearing $classesDir0")
      os.list(classesDir0).foreach { p =>
        logger.debug(s"Removing $p")
        os.remove.all(p)
      }
    }

    val diagnosticMappings = generatedSources
      .map {
        case (path, reportingPath, len) =>
          val lineShift = -os.read(path).take(len).count(_ == '\n') // charset?
          (path, (reportingPath, lineShift))
      }
      .toMap

    buildClient.clear()
    buildClient.generatedSources = diagnosticMappings
    val success = Bloop.compile(
      inputs.projectName,
      buildClient,
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
        updateSemanticDbs = generatedSrcRoot0 == defaultGeneratedSrcRoot(inputs.workspace, inputs.projectName),
        updateTasty = params.scalaVersion.startsWith("3.")
      )

      Successful(inputs, options, sources, artifacts, project, classesDir0, buildClient.diagnostics)
    }
    else
      Failed(inputs, options, sources, artifacts, project, buildClient.diagnostics)
  }

  private def postProcess(
    generatedSources: Seq[(os.Path, Either[String, os.Path], Int)],
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
        .map {
          case (path, reportingPath, len) =>
            val lineShift = -os.read(path).take(len).count(_ == '\n') // charset?
            (path.relativeTo(generatedSrcRoot).toString, (reportingPath.fold(s => s, _.last), lineShift))
        }
        .toMap
      AsmPositionUpdater.postProcess(mappings, classesDir, logger)

      if (updateSemanticDbs) {
        logger.debug("Moving semantic DBs around")
        val semDbRoot = classesDir / "META-INF" / "semanticdb"
        for ((path, Right(originalSource), offset) <- generatedSources) {
          val fromSourceRoot = path.relativeTo(workspace)
          val actual = originalSource.relativeTo(workspace)

          val semDbFile = semDbRoot / fromSourceRoot.segments.take(fromSourceRoot.segments.length - 1) / s"${fromSourceRoot.last}.semanticdb"
          if (os.exists(semDbFile)) {
            val finalSemDbFile = semDbRoot / actual.segments.take(actual.segments.length - 1) / s"${actual.last}.semanticdb"
            SemanticdbProcessor.postProcess(
              os.read(originalSource),
              originalSource.relativeTo(workspace),
              None,
              if (offset == 0) n => Some(n)
              else LineConversion.scalaLineToScLine(os.read(originalSource), os.read(path), offset),
              semDbFile,
              finalSemDbFile
            )
            os.remove(semDbFile)
          }
        }

        if (updateTasty) {
          val updatedPaths = generatedSources
            .collect {
              case (path, Right(originalSource), offset) =>
                val fromSourceRoot = path.relativeTo(workspace)
                val actual = originalSource.relativeTo(workspace)
                fromSourceRoot.toString -> actual.toString
            }
            .toMap

          if (updatedPaths.nonEmpty)
            os.walk(classesDir)
              .filter(os.isFile(_))
              .filter(_.last.endsWith(".tasty")) // make that case-insensitive just in case?
              .foreach { f =>
                logger.debug(s"Reading TASTy file $f")
                val content = os.read.bytes(f)
                val data = TastyData.read(content)
                logger.debug(s"Parsed TASTy file $f")
                var updatedOne = false
                val updatedData = data.mapNames { n =>
                  updatedPaths.get(n) match {
                    case Some(newName) =>
                      updatedOne = true
                      newName
                    case None =>
                      n
                  }
                }
                if (updatedOne) {
                  logger.debug(s"Overwriting ${if (f.startsWith(os.pwd)) f.relativeTo(os.pwd) else f}")
                  val updatedContent = TastyData.write(updatedData)
                  os.write.over(f, updatedContent)
                }
              }
        }
      } else
        logger.debug("Custom generated source directory used, not moving semantic DBs around")
  }

  private def onChangeBufferedObserver(onEvent: PathWatchers.Event => Unit): Observer[PathWatchers.Event] =
    new Observer[PathWatchers.Event] {
      def onError(t: Throwable): Unit = {
        // TODO Log that properly
        System.err.println("got error:")
        def printEx(t: Throwable): Unit =
          if (t != null) {
            System.err.println(t)
            System.err.println(t.getStackTrace.iterator.map("  " + _ + System.lineSeparator()).mkString)
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

    private val lock = new Object
    private var f: ScheduledFuture[_] = null
    private val waitFor = 50.millis
    private val runnable: Runnable = { () =>
      lock.synchronized {
        f = null
      }
      onChange
    }
    def schedule(): Unit =
      if (f == null)
        lock.synchronized {
          if (f == null)
            f = scheduler.schedule(runnable, waitFor.length, waitFor.unit)
        }
  }

  private def registerInputs(watcher: PathWatcher[PathWatchers.Event], inputs: Inputs): Unit =
    inputs.elements.foreach  {
      case elem: Inputs.OnDisk =>
        val depth = elem match {
          case _: Inputs.Directory => Int.MaxValue
          case _: Inputs.ResourceDirectory => Int.MaxValue
          case _ => -1
        }
        watcher.register(elem.path.toNIO, depth) match {
          case l: com.swoval.functional.Either.Left[IOException, JBoolean] => throw l.getValue
          case _ =>
        }
      case _: Inputs.Virtual =>
    }

  private def printable(path: os.Path): String =
    if (path.startsWith(os.pwd)) path.relativeTo(os.pwd).toString
    else path.toString

  private def jmhBuild(
    inputs: Inputs,
    build: Build.Successful,
    threads: BuildThreads,
    logger: Logger,
    cwd: os.Path,
    javaCommand: String,
    buildClient: BloopBuildClient,
    bloopServer: bloop.BloopServer
  ) = {
    val jmhProjectName = inputs.projectName + "_jmh"
    val jmhOutputDir = inputs.workspace / ".scala" / jmhProjectName
    os.remove.all(jmhOutputDir)
    val jmhSourceDir = jmhOutputDir / "sources"
    val jmhResourceDir = jmhOutputDir / "resources"

    val retCode = run(
      javaCommand,
      build.fullClassPath.map(_.toFile),
      "org.openjdk.jmh.generators.bytecode.JmhBytecodeGenerator",
      Seq(printable(build.output), printable(jmhSourceDir), printable(jmhResourceDir), "default"),
      logger
    )
    if (retCode != 0) {
      val red = Console.RED
      val lightRed = "\u001b[91m"
      val reset = Console.RESET
      System.err.println(s"${red}jmh bytecode generator exited with return code $lightRed$retCode$red.$reset")
    }

    if (retCode == 0) {
      val jmhInputs = inputs.copy(
        baseProjectName = jmhProjectName,
        mayAppendHash = false, // hash of the underlying project if needed is already in jmhProjectName
        tail = inputs.tail ++ Seq(
          Inputs.Directory(jmhSourceDir),
          Inputs.ResourceDirectory(jmhResourceDir)
        )
      )
      val updatedOptions = build.options.copy(
        jmhOptions = build.options.jmhOptions.copy(
          runJmh = build.options.jmhOptions.runJmh.map(_ => false)
        )
      )
      val jmhBuild = Build.build(jmhInputs, updatedOptions, threads, logger, cwd, buildClient, bloopServer)
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
        "-cp", classPath.iterator.map(_.getAbsolutePath).mkString(File.pathSeparator),
        mainClass
      ) ++
      args

    logger.log(
      s"Running ${command.mkString(" ")}",
      "  Running" + System.lineSeparator() + command.iterator.map(_ + System.lineSeparator()).mkString
    )

    new ProcessBuilder(command: _*)
      .inheritIO()
      .start()
      .waitFor()
  }

}
