package scala.build

import _root_.bloop.config.{Config => BloopConfig}
import ch.epfl.scala.bsp4j
import com.swoval.files.FileTreeViews.Observer
import com.swoval.files.{FileTreeRepositories, PathWatcher, PathWatchers}
import dependency._
import scala.build.bloop.bloopgun
import scala.build.internal.{AsmPositionUpdater, CodeWrapper, Constants, CustomCodeWrapper, LineConversion, MainClass, SemanticdbProcessor, Util}
import scala.build.internal.Constants._
import scala.build.tastylib.TastyData

import java.io.IOException
import java.lang.{Boolean => JBoolean}
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.nio.file.{Path, Paths}
import java.security.MessageDigest
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
    jsDependencies: Seq[AnyDependency],
    compilerPlugins: Seq[AnyDependency],
    config: BloopConfig.JsConfig
  )

  def scalaJsOptions(config: BloopConfig.JsConfig): ScalaJsOptions = {
    val version = config.version
    val platformSuffix = "sjs" + ScalaVersion.jsBinary(version).getOrElse(version)
    ScalaJsOptions(
      platformSuffix = platformSuffix,
      jsDependencies = Seq(
        dep"org.scala-js::scalajs-library:$version"
      ),
      compilerPlugins = Seq(
        dep"org.scala-js:::scalajs-compiler:$version"
      ),
      config = config
    )
  }

  final case class ScalaNativeOptions(
    platformSuffix: String,
    nativeDependencies: Seq[AnyDependency],
    compilerPlugins: Seq[AnyDependency],
    config: BloopConfig.NativeConfig
  )

  def scalaNativeOptions(config: BloopConfig.NativeConfig): ScalaNativeOptions = {
    val version = config.version
    val platformSuffix = "native" + ScalaVersion.nativeBinary(version).getOrElse(version)
    val nativeDeps = Seq("nativelib", "javalib", "auxlib", "scalalib")
      .map(name => dep"org.scala-native::$name::$version")
    Build.ScalaNativeOptions(
      platformSuffix = platformSuffix,
      nativeDependencies = nativeDeps,
      compilerPlugins = Seq(dep"org.scala-native:::nscplugin:$version"),
      config = config
    )
  }

  final case class Options(
    scalaVersion: Option[String],
    scalaBinaryVersion: Option[String],
    codeWrapper: Option[CodeWrapper] = None,
    scalaJsOptions: Option[ScalaJsOptions] = None,
    scalaNativeOptions: Option[ScalaNativeOptions] = None,
    javaHomeOpt: Option[String] = None,
    jvmIdOpt: Option[String] = None,
    addStubsDependencyOpt: Option[Boolean] = None,
    addRunnerDependencyOpt: Option[Boolean] = None,
    addTestRunnerDependencyOpt: Option[Boolean] = None,
    addJmhDependencies: Option[String] = None,
    runJmh: Option[RunJmhOptions] = None,
    addScalaLibrary: Option[Boolean] = None,
    generateSemanticDbs: Option[Boolean] = None,
    keepDiagnostics: Boolean = false,
    fetchSources: Option[Boolean] = None,
    extraRepositories: Seq[String] = Nil,
    extraJars: Seq[os.Path] = Nil
  ) {
    def classesDir(root: os.Path, projectName: String): os.Path =
      root / ".scala" / projectName / "classes"
    def generatedSrcRoot(root: os.Path, projectName: String) =
      defaultGeneratedSrcRoot(root, projectName)
    def defaultGeneratedSrcRoot(root: os.Path, projectName: String) =
      root / ".scala" / projectName / "src_generated"
    def addStubsDependency: Boolean =
      addStubsDependencyOpt.getOrElse(true)
    def addRunnerDependency: Boolean =
      scalaJsOptions.isEmpty && scalaNativeOptions.isEmpty && addRunnerDependencyOpt.getOrElse(true)
    def addTestRunnerDependency: Boolean =
      addTestRunnerDependencyOpt.getOrElse(false)

    // lazy val params = ScalaParameters(scalaVersion, scalaBinaryVersion)

    def scalaLibraryDependencies(params: ScalaParameters): Seq[AnyDependency] =
      if (addScalaLibrary.getOrElse(true)) {
        val lib =
          if (params.scalaVersion.startsWith("3."))
            dep"org.scala-lang::scala3-library:${params.scalaVersion}"
          else
            dep"org.scala-lang:scala-library:${params.scalaVersion}"
        Seq(lib)
      }
      else Nil

    def dependencies(params: ScalaParameters): Seq[AnyDependency] =
      scalaJsOptions.map(_.jsDependencies).getOrElse(Nil) ++
        scalaNativeOptions.map(_.nativeDependencies).getOrElse(Nil) ++
        scalaLibraryDependencies(params)

    def semanticDbPlugins(params: ScalaParameters): Seq[AnyDependency] =
      if (generateSemanticDbs.getOrElse(false) && params.scalaVersion.startsWith("2."))
        Seq(
          dep"$semanticDbPluginOrganization:::$semanticDbPluginModuleName:$semanticDbPluginVersion"
        )
      else Nil

    def compilerPlugins(params: ScalaParameters): Seq[AnyDependency] =
      scalaJsOptions.map(_.compilerPlugins).getOrElse(Nil) ++
        scalaNativeOptions.map(_.compilerPlugins).getOrElse(Nil) ++
        semanticDbPlugins(params)

    def allExtraJars: Seq[Path] =
      extraJars.map(_.toNIO)

    def addJvmTestRunner: Boolean = scalaJsOptions.isEmpty && scalaNativeOptions.isEmpty && addTestRunnerDependency
    def addJsTestBridge: Option[String] = if (addTestRunnerDependency) scalaJsOptions.map(_.config.version) else None

    def artifacts(params: ScalaParameters, userDependencies: Seq[AnyDependency], logger: Logger): Artifacts =
      Artifacts(
        javaHomeOpt = javaHomeOpt.filter(_.nonEmpty),
        jvmIdOpt = jvmIdOpt,
        params = params,
        compilerPlugins = compilerPlugins(params),
        dependencies = userDependencies ++ dependencies(params),
        extraJars = allExtraJars,
        fetchSources = fetchSources.getOrElse(false),
        addStubs = addStubsDependency,
        addJvmRunner = addRunnerDependency,
        addJvmTestRunner = addJvmTestRunner,
        addJsTestBridge = addJsTestBridge,
        addJmhDependencies = addJmhDependencies,
        extraRepositories = extraRepositories,
        logger = logger
      )

    lazy val hash: Option[String] = {
      val md = MessageDigest.getInstance("SHA-1")

      var hasAnyOverride = false

      def update(s: String): Unit = {
        val bytes = s.getBytes(StandardCharsets.UTF_8)
        if (bytes.length > 0) {
          hasAnyOverride = true
          md.update(bytes)
        }
      }
      for (sv <- scalaVersion)
        update("scalaVersion=" + sv + "\n")
      for (sbv <- scalaBinaryVersion)
        update("scalaBinaryVersion=" + sbv + "\n")
      for (wrapper <- codeWrapper)
        // kind of meh to use wrapper.toString here…
        update("codeWrapper=" + wrapper.toString + "\n")
      for (jsOpts <- scalaJsOptions) {
        update("js=" + jsOpts.config.version + "\n")
        update("js.suffix=" + jsOpts.platformSuffix + "\n")
        for (plugin <- jsOpts.compilerPlugins)
          update("js.compilerPlugin+=" + plugin.render + "\n")
        for (dep <- jsOpts.jsDependencies)
          update("js.jsDeps+=" + dep.render + "\n")
      }
      for (nativeOpts <- scalaNativeOptions) {
        update("native=" + nativeOpts.config.version + "\n")
        update("native.suffix=" + nativeOpts.platformSuffix + "\n")
        for (plugin <- nativeOpts.compilerPlugins)
          update("native.compilerPlugin+=" + plugin.render + "\n")
        for (dep <- nativeOpts.nativeDependencies)
          update("native.nativeDeps+=" + dep.render + "\n")
      }

      for (home <- javaHomeOpt)
        update("javaHome=" + home + "\n")
      for (id <- jvmIdOpt)
        update("jvmId=" + id + "\n")

      for (add <- addStubsDependencyOpt)
        update("addStubsDependency=" + add.toString + "\n")
      for (add <- addRunnerDependencyOpt)
        update("addRunnerDependency=" + add.toString + "\n")
      for (add <- addTestRunnerDependencyOpt)
        update("addTestRunnerDependency=" + add.toString + "\n")

      for (dep <- addJmhDependencies)
        update("addJmhDependencies=" + dep + "\n")
      for (add <- addScalaLibrary)
        update("addScalaLibrary=" + add.toString + "\n")
      for (generate <- generateSemanticDbs)
        update("generateSemanticDbs=" + generate.toString + "\n")

      for (jar <- extraJars)
        update("jars+=" + jar.toString + "\n")

      if (hasAnyOverride) {
        val digest = md.digest()
        val calculatedSum = new BigInteger(1, digest)
        val hash = String.format(s"%040x", calculatedSum).take(10)
        Some(hash)
      }
      else None
    }
  }

  private def computeScalaVersions(scalaVersion: Option[String], scalaBinaryVersion: Option[String]): (String, String) = {
    import coursier.core.Version
    lazy val allVersions = {
      import coursier._
      import scala.concurrent.ExecutionContext.{global => ec}
      val modules = {
        def scala2 = mod"org.scala-lang:scala-library"
        // No unstable, that *ought* not to be a problem down-the-line…?
        def scala3 = mod"org.scala-lang:scala3-library_3"
        if (scalaVersion.contains("2") || scalaVersion.exists(_.startsWith("2."))) Seq(scala2)
        else if (scalaVersion.contains("3") || scalaVersion.exists(_.startsWith("3."))) Seq(scala3)
        else Seq(scala2, scala3)
      }
      def isStable(v: String): Boolean =
        !v.endsWith("-NIGHTLY") && !v.contains("-RC")
      def moduleVersions(mod: Module): Seq[String] = {
        val res = Versions()
          .withModule(mod)
          .result()
          .unsafeRun()(ec)
        res.versions.available.filter(isStable)
      }
      modules.flatMap(moduleVersions).distinct
    }
    val sv = scalaVersion match {
      case None => scala.util.Properties.versionNumberString
      case Some(sv0) =>
        if (Util.isFullScalaVersion(sv0)) sv0
        else {
          val prefix = if (sv0.endsWith(".")) sv0 else sv0 + "."
          val matchingVersions = allVersions.filter(_.startsWith(prefix))
          if (matchingVersions.isEmpty)
            sys.error(s"Cannot find matching Scala version for '$sv0'")
          else
            matchingVersions.map(Version(_)).max.repr
        }
    }
    val sbv = scalaBinaryVersion.getOrElse(ScalaVersion.binary(sv))
    (sv, sbv)
  }

  final case class RunJmhOptions(
    preprocess: Boolean,
    javaCommand: String
  )

  private def build(
    inputs: Inputs,
    options: Options,
    threads: BuildThreads,
    logger: Logger,
    cwd: os.Path,
    buildClient: BloopBuildClient,
    bloopServer: bloop.BloopServer
  ): Build = {

    val sources = Sources.forInputs(
      inputs,
      options.codeWrapper.getOrElse(CustomCodeWrapper)
    )

    // at some point, we'll allow to override some options from sources here
    val options0 = options

    // If some options are manually overridden, append a hash of the options to the project name
    val inputs0 = inputs.copy(
      baseProjectName = inputs.baseProjectName + options.hash.map("_" + _).getOrElse("")
    )

    val params = {
      val (scalaVersion, scalaBinaryVersion) = computeScalaVersions(options0.scalaVersion, options0.scalaBinaryVersion)
      val maybePlatformSuffix =
        options0.scalaJsOptions.map(_.platformSuffix)
          .orElse(options0.scalaNativeOptions.map(_.platformSuffix))
      ScalaParameters(scalaVersion, scalaBinaryVersion, maybePlatformSuffix)
    }

    val build0 = buildOnce(cwd, inputs0, params, sources, options0, threads, logger, buildClient, bloopServer)

    build0 match {
      case successful: Successful =>
        options0.runJmh match {
          case Some(runJmhOptions) if runJmhOptions.preprocess =>
            jmhBuild(inputs0, successful, threads, logger, cwd, runJmhOptions.javaCommand, buildClient, bloopServer).getOrElse {
              sys.error("JMH build failed") // suppress stack trace?
            }
          case _ => build0
        }
      case _ => build0
    }
  }

  def build(
    inputs: Inputs,
    options: Options,
    threads: BuildThreads,
    bloopConfig: bloopgun.BloopgunConfig,
    logger: Logger,
    cwd: os.Path
  ): Build = {

    val buildClient = new BloopBuildClient(
      logger,
      keepDiagnostics = options.keepDiagnostics
    )
    val classesDir = options.classesDir(inputs.workspace, inputs.projectName)
    bloop.BloopServer.withBuildServer(
      bloopConfig,
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
    bloopConfig: bloopgun.BloopgunConfig,
    logger: Logger,
    cwd: os.Path
  ): Build =
    build(inputs, options, BuildThreads.create(), bloopConfig, logger, cwd)

  def watch(
    inputs: Inputs,
    options: Options,
    bloopConfig: bloopgun.BloopgunConfig,
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
      bloopConfig,
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
    options: Options,
    threads: BuildThreads,
    logger: Logger,
    buildClient: BloopBuildClient,
    bloopServer: bloop.BloopServer
  ): Build = {

    val generatedSrcRoot = options.generatedSrcRoot(inputs.workspace, inputs.projectName)
    val generatedSources = sources.generateSources(generatedSrcRoot)
    val allSources = sources.paths.map(_._1) ++ generatedSources.map(_._1)

    val classesDir = options.classesDir(inputs.workspace, inputs.projectName)

    val artifacts = options.artifacts(params, sources.dependencies, logger)

    val pluginScalacOptions = artifacts.compilerPlugins.map {
      case (_, _, path) =>
        s"-Xplugin:${path.toAbsolutePath}"
    }

    val semanticDbScalacOptions =
      if (options.generateSemanticDbs.getOrElse(false)) {
        if (params.scalaVersion.startsWith("2."))
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
      if (params.scalaVersion.startsWith("2.")) Nil
      else Seq("-sourceroot", inputs.workspace.toString)

    val scalacOptions = Seq("-encoding", "UTF-8", "-deprecation", "-feature") ++
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
               workspace = inputs.workspace,
              classesDir = classesDir,
           scalaCompiler = scalaCompiler,
          scalaJsOptions = options.scalaJsOptions.map(_.config),
      scalaNativeOptions = options.scalaNativeOptions.map(_.config),
             projectName = inputs.projectName,
               classPath = artifacts.classPath,
                 sources = allSources,
            resourceDirs = sources.resourceDirs
    )

    if (project.writeBloopFile(logger) && os.isDir(classesDir)) {
      logger.debug(s"Clearing $classesDir")
      os.list(classesDir).foreach { p =>
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
      logger
    )

    if (success) {
      postProcess(
        generatedSources,
        generatedSrcRoot,
        classesDir,
        logger,
        inputs.workspace,
        updateSemanticDbs = generatedSrcRoot == options.defaultGeneratedSrcRoot(inputs.workspace, inputs.projectName),
        updateTasty = params.scalaVersion.startsWith("3.")
      )

      Successful(inputs, options, sources, artifacts, project, classesDir, buildClient.diagnostics)
    }
    else
      Failed(inputs, options, sources, artifacts, project, buildClient.diagnostics)
  }

  private def postProcess(
    generatedSources: Seq[(os.Path, os.Path, Int)],
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
            (path.relativeTo(generatedSrcRoot).toString, (reportingPath.last, lineShift))
        }
        .toMap
      AsmPositionUpdater.postProcess(mappings, classesDir, logger)

      if (updateSemanticDbs) {
        logger.debug("Moving semantic DBs around")
        val semDbRoot = classesDir / "META-INF" / "semanticdb"
        for ((path, originalSource, offset) <- generatedSources) {
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
            .map {
              case (path, originalSource, offset) =>
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

    val retCode = Runner.run(
      javaCommand,
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
      val jmhBuild = Build.build(jmhInputs, build.options.copy(runJmh = build.options.runJmh.map(_.copy(preprocess = false))), threads, logger, cwd, buildClient, bloopServer)
      Some(jmhBuild)
    }
    else None
  }

}
