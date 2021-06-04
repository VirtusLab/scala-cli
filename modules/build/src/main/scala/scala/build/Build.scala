package scala.build

import ch.epfl.scala.bsp4j
import com.swoval.files.FileTreeViews.Observer
import com.swoval.files.{FileTreeRepositories, PathWatcher, PathWatchers}
import dependency._
import scala.build.internal.{AsmPositionUpdater, CodeWrapper, Constants, CustomCodeWrapper, LineConversion, MainClass, SemanticdbProcessor, Util}
import scala.build.internal.Constants._
import scala.build.internal.Util.{DependencyOps, ScalaDependencyOps}
import scala.build.tastylib.TastyData

import java.io.IOException
import java.lang.{Boolean => JBoolean}
import java.nio.file.{Path, Paths}
import java.util.concurrent.{ExecutorService, ScheduledExecutorService, ScheduledFuture}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.scalanative.{build => sn}
import scala.util.control.NonFatal

trait Build {
  def inputs: Inputs
  def options: Build.Options
  def sources: Sources
  def artifacts: Artifacts
  def project: Project
  def outputOpt: Option[os.Path]
  def success: Boolean
  def diagnostics: Option[Seq[(os.Path, bsp4j.Diagnostic)]]

  def successfulOpt: Option[Build.Successful]
}

object Build {

  final case class Successful(
    inputs: Inputs,
    options: Build.Options,
    sources: Sources,
    artifacts: Artifacts,
    project: Project,
    output: os.Path,
    diagnostics: Option[Seq[(os.Path, bsp4j.Diagnostic)]]
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
    options: Build.Options,
    sources: Sources,
    artifacts: Artifacts,
    project: Project,
    diagnostics: Option[Seq[(os.Path, bsp4j.Diagnostic)]]
  ) extends Build {
    def success: Boolean = false
    def successfulOpt: None.type = None
    def outputOpt: None.type = None
  }

  final case class ScalaJsOptions(
    platformSuffix: String,
    jsDependencies: Seq[coursierapi.Dependency],
    compilerPlugins: Seq[coursierapi.Dependency],
    config: Project.ScalaJsOptions
  ) {
    def version: String = config.version
  }

  def scalaJsOptions(scalaVersion: String, scalaBinaryVersion: String): ScalaJsOptions = {
    val version = Constants.scalaJsVersion
    val platformSuffix = "sjs" + ScalaVersion.jsBinary(version).getOrElse(version)
    val params = ScalaParameters(scalaVersion, scalaBinaryVersion, Some(platformSuffix))
    ScalaJsOptions(
      platformSuffix = platformSuffix,
      jsDependencies = Seq(
        dep"org.scala-js::scalajs-library:$version".toApi(params)
      ),
      compilerPlugins = Seq(
        dep"org.scala-js:::scalajs-compiler:$version".toApi(params)
      ),
      config = Project.ScalaJsOptions(
        version = version,
        mode = "debug"
      )
    )
  }

  final case class ScalaNativeOptions(
    platformSuffix: String,
    nativeDependencies: Seq[coursierapi.Dependency],
    compilerPlugins: Seq[coursierapi.Dependency],
    config: Project.ScalaNativeOptions
  ) {
    def version: String = config.version
  }

  def scalaNativeOptions(scalaVersion: String, scalaBinaryVersion: String): ScalaNativeOptions = {
    val version = Constants.scalaNativeVersion
    val platformSuffix = "native" + ScalaVersion.nativeBinary(version).getOrElse(version)
    val params = ScalaParameters(scalaVersion, scalaBinaryVersion, Some(platformSuffix))
    val nativeDeps = Seq("nativelib", "javalib", "auxlib", "scalalib")
      .map(name => dep"org.scala-native::$name::$version".toApi(params))
    Build.ScalaNativeOptions(
      platformSuffix = platformSuffix,
      nativeDependencies = nativeDeps,
      compilerPlugins = Seq(dep"org.scala-native:::nscplugin:$version".toApi(params)),
      config = Project.ScalaNativeOptions(
               version = version,
                  mode = "debug",
                    gc = "default",
                 clang = sn.Discover.clang(),
               clangpp = sn.Discover.clangpp(),
        linkingOptions = sn.Discover.linkingOptions(),
        compileOptions = sn.Discover.compileOptions()
      )
    )
  }

  final case class Options(
    scalaVersion: String,
    scalaBinaryVersion: String,
    generatedSrcRootOpt: Option[os.Path] = None,
    codeWrapper: CodeWrapper = CustomCodeWrapper,
    scalaJsOptions: Option[ScalaJsOptions] = None,
    scalaNativeOptions: Option[ScalaNativeOptions] = None,
    javaHomeOpt: Option[String] = None,
    jvmIdOpt: Option[String] = None,
    stubsJarOpt: Option[Path] = None,
    addStubsDependencyOpt: Option[Boolean] = None,
    testRunnerJarsOpt: Option[Seq[Path]] = None,
    addRunnerDependencyOpt: Option[Boolean] = None,
    addTestRunnerDependencyOpt: Option[Boolean] = None,
    addJmhDependencies: Option[String] = None,
    runJmh: Boolean = false,
    addLineModifierPluginOpt: Option[Boolean] = None,
    addScalaLibrary: Boolean = true,
    generateSemanticDbs: Boolean = false,
    keepDiagnostics: Boolean = false
  ) {
    def classesDir(root: os.Path, projectName: String): os.Path =
      root / ".scala" / projectName / s"scala-$scalaVersion" / "classes"
    def generatedSrcRoot(root: os.Path, projectName: String) = generatedSrcRootOpt.getOrElse {
      defaultGeneratedSrcRoot(root, projectName)
    }
    def defaultGeneratedSrcRoot(root: os.Path, projectName: String) =
      root / ".scala" / projectName / s"scala-$scalaVersion" / "src_generated"
    def addStubsDependency: Boolean =
      addStubsDependencyOpt.getOrElse(stubsJarOpt.isEmpty)
    def addRunnerDependency: Boolean =
      scalaJsOptions.isEmpty && scalaNativeOptions.isEmpty && addRunnerDependencyOpt.getOrElse(true)
    def addTestRunnerDependency: Boolean =
      addTestRunnerDependencyOpt.getOrElse(false)
    def addLineModifierPlugin: Boolean =
      addLineModifierPluginOpt.getOrElse {
        false
      }
  }

  private def build(
    inputs: Inputs,
    options: Options,
    threads: BuildThreads,
    logger: Logger,
    cwd: os.Path,
    buildClient: BloopBuildClient,
    bloopServer: bloop.BloopServer
  ): Build = {
    val maybePlatformSuffix =
      options.scalaJsOptions.map(_.platformSuffix)
        .orElse(options.scalaNativeOptions.map(_.platformSuffix))
    val sources = Sources.forInputs(
      inputs,
      options.codeWrapper,
      maybePlatformSuffix.getOrElse(""),
      options.scalaVersion,
      options.scalaBinaryVersion
    )
    val build0 = buildOnce(cwd, inputs, sources, options, threads, logger, buildClient, bloopServer)

    build0 match {
      case successful: Successful if options.runJmh =>
        jmhBuild(inputs, successful, threads, logger, cwd, buildClient, bloopServer).getOrElse {
          sys.error("JMH build failed") // suppress stack trace?
        }
      case _ => build0
    }
  }

  def build(
    inputs: Inputs,
    options: Options,
    threads: BuildThreads,
    logger: Logger,
    cwd: os.Path
  ): Build = {

    val buildClient = new BloopBuildClient(
      logger,
      keepDiagnostics = options.keepDiagnostics
    )
    val classesDir = options.classesDir(inputs.workspace, inputs.projectName)
    bloop.BloopServer.withBuildServer(
      "scala-cli",
      Constants.version,
      inputs.workspace.toNIO,
      classesDir.toNIO,
      buildClient,
      threads.bloop,
      logger.bloopgunLogger
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
    options: Options,
    logger: Logger,
    cwd: os.Path
  ): Build =
    build(inputs, options, BuildThreads.create(), logger, cwd)

  def watch(
    inputs: Inputs,
    options: Options,
    logger: Logger,
    cwd: os.Path,
    postAction: () => Unit = () => ()
  )(action: Build => Unit): Watcher = {

    val buildClient = new BloopBuildClient(
      logger,
      keepDiagnostics = options.keepDiagnostics
    )
    val threads = BuildThreads.create()
    val classesDir = options.classesDir(inputs.workspace, inputs.projectName)
    val bloopServer = bloop.BloopServer.buildServer(
      "scala-cli",
      Constants.version,
      inputs.workspace.toNIO,
      classesDir.toNIO,
      buildClient,
      threads.bloop,
      logger.bloopgunLogger
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
              !isHidden && (isScalaFile || isJavaFile)
          case _ => _ => true
        }

        val watcher0 = watcher.newWatcher()
        watcher0.register(elem.path.toNIO, depth)
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
    sources: Sources,
    options: Options,
    threads: BuildThreads,
    logger: Logger,
    buildClient: BloopBuildClient,
    bloopServer: bloop.BloopServer
  ): Build = {

    val generatedSrcRoot = options.generatedSrcRoot(inputs.workspace, inputs.projectName)
    val generatedSources = sources.generateSources(generatedSrcRoot)
    val allSources = sources.paths ++ generatedSources.map(_._1)
    val allScalaSources = allSources

    val classesDir = options.classesDir(inputs.workspace, inputs.projectName)

    val params = ScalaParameters(options.scalaVersion, options.scalaBinaryVersion)
    val scalaLibraryDependencies =
      if (options.addScalaLibrary) {
        val lib =
          if (options.scalaVersion.startsWith("3."))
            dep"org.scala-lang::scala3-library:${options.scalaVersion}".toApi(params)
          else
            dep"org.scala-lang:scala-library:${options.scalaVersion}".toApi
        Seq(lib)
      }
      else Nil

    val allDependencies =
      sources.dependencies ++
        options.scalaJsOptions.map(_.jsDependencies).getOrElse(Nil) ++
        options.scalaNativeOptions.map(_.nativeDependencies).getOrElse(Nil) ++
        scalaLibraryDependencies

    val lineModifierPlugins =
      if (options.addLineModifierPlugin)
        Seq(
          dep"$lineModifierPluginOrganization::$lineModifierPluginModuleName:$lineModifierPluginVersion".toApi(params)
        )
      else Nil

    val semanticDbPlugins =
      if (options.generateSemanticDbs && options.scalaVersion.startsWith("2."))
        Seq(
          dep"$semanticDbPluginOrganization:::$semanticDbPluginModuleName:$semanticDbPluginVersion".toApi(params)
        )
      else Nil

    val artifacts = Artifacts(
      options.javaHomeOpt.filter(_.nonEmpty),
      options.jvmIdOpt,
      options.scalaVersion,
      options.scalaBinaryVersion,
      options.scalaJsOptions.map(_.compilerPlugins).getOrElse(Nil) ++
        options.scalaNativeOptions.map(_.compilerPlugins).getOrElse(Nil) ++
        lineModifierPlugins ++
        semanticDbPlugins,
      allDependencies,
      options.stubsJarOpt.toSeq ++ options.testRunnerJarsOpt.getOrElse(Nil),
      addStubs = options.addStubsDependency,
      addJvmRunner = options.addRunnerDependency,
      addJvmTestRunner = options.scalaJsOptions.isEmpty && options.scalaNativeOptions.isEmpty && options.addTestRunnerDependency,
      addJsTestBridge = if (options.addTestRunnerDependency) options.scalaJsOptions.map(_.version) else None,
      addJmhDependencies = options.addJmhDependencies,
      logger = logger
    )

    val pluginScalacOptions = artifacts.compilerPlugins.map {
      case (_, _, path) =>
        s"-Xplugin:${path.toAbsolutePath}"
    }

    val lineModifierScalacOptions =
      if (options.addLineModifierPlugin) {
        val lengths = generatedSources
          .map {
            case (path, reportingPath, len) =>
              s"$path->$reportingPath=$len"
          }
          .mkString(";")
        Seq(s"-P:linemodifier:topWrapperLengths=$lengths")
      }
      else Nil

    val semanticDbScalacOptions =
      if (options.generateSemanticDbs) {
        if (options.scalaVersion.startsWith("2."))
          Seq(
            "-Yrangepos",
            "-P:semanticdb:failures:warning",
            "-P:semanticdb:synthetics:on",
            s"-P:semanticdb:sourceroot:${inputs.workspace}",
            s"-P:semanticdb:targetroot:${classesDir}"
          )
        else
          Seq(
            "-Xsemanticdb",
            "-semanticdb-target", classesDir.toString
          )
      }
      else Nil

    val sourceRootScalacOptions =
      if (options.scalaVersion.startsWith("2.")) Nil
      else Seq("-sourceroot", inputs.workspace.toString)

    val scalacOptions = Seq("-encoding", "UTF-8", "-deprecation", "-feature") ++
      pluginScalacOptions ++
      lineModifierScalacOptions ++
      semanticDbScalacOptions ++
      sourceRootScalacOptions

    val scalaCompiler = ScalaCompiler(
            scalaVersion = options.scalaVersion,
      scalaBinaryVersion = options.scalaBinaryVersion,
           scalacOptions = scalacOptions,
       compilerClassPath = artifacts.compilerClassPath
    )

    val project = Project(
               workspace = inputs.workspace,
              classesDir = classesDir,
                javaHome = artifacts.javaHome,
           scalaCompiler = scalaCompiler,
          scalaJsOptions = options.scalaJsOptions.map(_.config),
      scalaNativeOptions = options.scalaNativeOptions.map(_.config),
             projectName = inputs.projectName,
               classPath = artifacts.classPath,
                 sources = allScalaSources,
            resourceDirs = sources.resourceDirs
    )

    project.writeBloopFile(logger)

    val diagnosticMappings = generatedSources
      .map {
        case (path, reportingPath, len) =>
          val lineShift =
            if (options.addLineModifierPlugin) 0
            else -os.read(path).take(len).count(_ == '\n') // charset?
          (path, (reportingPath, lineShift))
      }
      .toMap

    buildClient.clear()
    buildClient.generatedSources = diagnosticMappings
    val success = Bloop.compile(
      inputs.projectName,
      buildClient,
      bloopServer,
      logger
    )

    if (success) {
      // TODO Disable post-processing altogether when options.addLineModifierPlugin is true
      // (needs source gen to use the exact same names as the original file)
      // TODO Write classes to a separate directory during post-processing
      logger.debug("Post-processing class files of pre-processed sources")
      val mappings = generatedSources
        .map {
          case (path, reportingPath, len) =>
            val lineShift =
              if (options.addLineModifierPlugin) 0
              else -os.read(path).take(len).count(_ == '\n') // charset?
            (path.relativeTo(generatedSrcRoot).toString, (reportingPath.last, lineShift))
        }
        .toMap
      AsmPositionUpdater.postProcess(mappings, classesDir, logger)

      if (generatedSrcRoot == options.defaultGeneratedSrcRoot(inputs.workspace, inputs.projectName)) {
        logger.debug("Moving semantic DBs around")
        val semDbRoot = classesDir / "META-INF" / "semanticdb"
        for ((path, originalSource, offset) <- generatedSources) {
          val fromSourceRoot = path.relativeTo(inputs.workspace)
          val actual = originalSource.relativeTo(inputs.workspace)

          val semDbFile = semDbRoot / fromSourceRoot.segments.take(fromSourceRoot.segments.length - 1) / s"${fromSourceRoot.last}.semanticdb"
          if (os.exists(semDbFile)) {
            val finalSemDbFile = semDbRoot / actual.segments.take(actual.segments.length - 1) / s"${actual.last}.semanticdb"
            SemanticdbProcessor.postProcess(
              os.read(originalSource),
              originalSource.relativeTo(inputs.workspace),
              None,
              if (offset == 0) n => Some(n)
              else LineConversion.scalaLineToScLine(os.read(originalSource), os.read(path), offset),
              semDbFile,
              finalSemDbFile
            )
            os.remove(semDbFile)
          }
        }

        if (options.scalaVersion.startsWith("3.")) {
          val updatedPaths = generatedSources
            .map {
              case (path, originalSource, offset) =>
                val fromSourceRoot = path.relativeTo(inputs.workspace)
                val actual = originalSource.relativeTo(inputs.workspace)
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

      Successful(inputs, options, sources, artifacts, project, classesDir, buildClient.diagnostics)
    }
    else
      Failed(inputs, options, sources, artifacts, project, buildClient.diagnostics)
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
    for (elem <- inputs.elements) {
      val depth = elem match {
        case _: Inputs.Directory => Int.MaxValue
        case _: Inputs.ResourceDirectory => Int.MaxValue
        case _ => -1
      }
      watcher.register(elem.path.toNIO, depth) match {
        case l: com.swoval.functional.Either.Left[IOException, JBoolean] => throw l.getValue
        case _ =>
      }
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
    buildClient: BloopBuildClient,
    bloopServer: bloop.BloopServer
  ) = {
    val jmhProjectName = inputs.projectName + "_jmh"
    val jmhOutputDir = inputs.workspace / ".scala" / jmhProjectName
    os.remove.all(jmhOutputDir)
    val jmhSourceDir = jmhOutputDir / "sources"
    val jmhResourceDir = jmhOutputDir / "resources"

    val retCode = Runner.run(
      build.artifacts.javaHome.toIO,
      Nil,
      build.fullClassPath.map(_.toFile),
      "org.openjdk.jmh.generators.bytecode.JmhBytecodeGenerator",
      Seq(printable(build.output), printable(jmhSourceDir), printable(jmhResourceDir), "default"),
      logger,
      allowExecve = false
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
      val jmhBuild = Build.build(jmhInputs, build.options.copy(runJmh = false), threads, logger, cwd, buildClient, bloopServer)
      Some(jmhBuild)
    }
    else None
  }

}
