package scala.cli

import com.swoval.files.FileTreeViews.Observer
import com.swoval.files.{FileTreeRepositories, PathWatcher, PathWatchers}
import scala.cli.internal.{CodeWrapper, Constants, CustomCodeWrapper, MainClass, Util}

import java.io.IOException
import java.lang.{Boolean => JBoolean}
import java.nio.file.{Path, Paths}
import java.util.concurrent.{ScheduledExecutorService, ScheduledFuture}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.scalanative.{build => sn}
import scala.util.control.NonFatal

final case class Build(
  inputs: Inputs,
  options: Build.Options,
  sources: Sources,
  artifacts: Artifacts,
  project: Project,
  output: Path
) {
  def fullClassPath: Seq[Path] =
    Seq(output) ++ sources.resourceDirs.map(_.toNIO) ++ artifacts.classPath
  def foundMainClasses(): Seq[String] =
    MainClass.find(os.Path(output))
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

object Build {

  final case class ScalaJsOptions(
    platformSuffix: String,
    jsDependencies: Seq[coursierapi.Dependency],
    compilerPlugins: Seq[coursierapi.Dependency],
    config: Project.ScalaJsOptions
  ) {
    def version: String = config.version
  }

  def scalaJsOptions(scalaVersion: String, scalaBinaryVersion: String): ScalaJsOptions = {
    val version = scala.cli.internal.Constants.scalaJsVersion
    val platformSuffix = "_sjs" + version.split('.').take(if (version.startsWith("0.")) 2 else 1).mkString(".") // meh
    val jsDeps = Seq(
      coursierapi.Dependency.of("org.scala-js", "scalajs-library_" + scalaBinaryVersion, version)
    )
    ScalaJsOptions(
      platformSuffix = platformSuffix,
      jsDependencies = jsDeps,
      compilerPlugins = Seq(coursierapi.Dependency.of("org.scala-js", "scalajs-compiler_" + scalaVersion, version)),
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
    val version = scala.cli.internal.Constants.scalaNativeVersion
    val platformSuffix = "_native" + version.split('.').take(2).mkString(".") // meh
    val nativeDeps = Seq("nativelib", "javalib", "auxlib", "scalalib")
      .map(_ + platformSuffix + "_" + scalaBinaryVersion)
      .map(name => coursierapi.Dependency.of("org.scala-native", name, version))
    Build.ScalaNativeOptions(
      platformSuffix = platformSuffix,
      nativeDependencies = nativeDeps,
      compilerPlugins = Seq(coursierapi.Dependency.of("org.scala-native", "nscplugin_" + scalaVersion, version)),
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
    addLineModifierPlugin: Boolean = true
  ) {
    def generatedSrcRoot(root: os.Path, projectName: String) = generatedSrcRootOpt.getOrElse {
      root / ".scala" / ".bloop" / projectName / ".src_generated"
    }
    def addStubsDependency: Boolean =
      addStubsDependencyOpt.getOrElse(stubsJarOpt.isEmpty)
    def addRunnerDependency: Boolean =
      scalaJsOptions.isEmpty && scalaNativeOptions.isEmpty && addRunnerDependencyOpt.getOrElse(true)
    def addTestRunnerDependency: Boolean =
      addTestRunnerDependencyOpt.getOrElse(false)
  }

  def build(
    inputs: Inputs,
    options: Options,
    logger: Logger,
    cwd: os.Path
  ): Build = {
    val maybePlatformSuffix =
      options.scalaJsOptions.map(_.platformSuffix)
        .orElse(options.scalaNativeOptions.map(_.platformSuffix))
    val sources = Sources(
      cwd,
      inputs,
      options.codeWrapper,
      maybePlatformSuffix.getOrElse(""),
      options.scalaVersion,
      options.scalaBinaryVersion
    )
    val build0 = buildOnce(cwd, inputs, sources, options, logger)

    if (options.runJmh)
      jmhBuild(inputs, build0, logger, cwd).getOrElse {
        sys.error("JMH build failed") // suppress stack trace?
      }
    else
      build0
  }

  def watch(
    inputs: Inputs,
    options: Options,
    logger: Logger,
    cwd: os.Path,
    postAction: () => Unit = () => ()
  )(action: Build => Unit): Watcher = {

    def run() = {
      try {
        val build0 = build(inputs, options, logger, cwd)
        action(build0)
      } catch {
        case NonFatal(e) =>
          Util.printException(e)
      }
      postAction()
    }

    run()

    val watcher = new Watcher(
      ListBuffer(),
      coursier.cache.internal.ThreadUtil.fixedScheduledThreadPool(1), // FIXME Tweak thread names?
      run()
    )

    try {
      for (elem <- inputs.elements) {
        val depth = elem match {
          case s: Inputs.SingleFile => -1
          case _ => Int.MaxValue
        }
        val eventFilter: PathWatchers.Event => Boolean = elem match {
          case d: Inputs.Directory =>
            val dir = os.FilePath(d.path).resolveFrom(cwd)

            // Filtering event for directories, to ignore those related to the .bloop directory in particular
            event =>
              val p = os.Path(event.getTypedPath.getPath.toAbsolutePath)
              val relPath = p.relativeTo(dir)
              val isHidden = relPath.segments.exists(_.startsWith("."))
              def isScalaFile = relPath.last.endsWith(".sc") || relPath.last.endsWith(".scala")
              !isHidden && isScalaFile
          case _ => _ => true
        }

        val watcher0 = watcher.newWatcher()
        watcher0.register(os.FilePath(elem.path).resolveFrom(cwd).toNIO, depth)
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
    logger: Logger
  ): Build = {

    val generatedSources = sources.generateSources(options.generatedSrcRoot(inputs.workspace, inputs.projectName))
    val allSources =
      sources.paths.map(p => os.Path(sourceRoot.toNIO.resolve(p).toAbsolutePath)) ++
        generatedSources.map(_._1)
    val allScalaSources = allSources

    val scalaLibraryDependencies =
      if (options.scalaVersion.startsWith("3."))
        Seq(coursierapi.Dependency.of("org.scala-lang", "scala3-library_" + options.scalaBinaryVersion, options.scalaVersion))
      else
        Seq(coursierapi.Dependency.of("org.scala-lang", "scala-library", options.scalaVersion))

    val allDependencies =
      sources.dependencies ++
        options.scalaJsOptions.map(_.jsDependencies).getOrElse(Nil) ++
        options.scalaNativeOptions.map(_.nativeDependencies).getOrElse(Nil) ++
        scalaLibraryDependencies

    val extraPlugins =
      if (options.addLineModifierPlugin)
        Seq(coursierapi.Dependency.of(
          Constants.lineModifierPluginOrganization,
          Constants.lineModifierPluginModuleName + "_" + options.scalaBinaryVersion,
          Constants.lineModifierPluginVersion
        ))
      else Nil

    val artifacts = Artifacts(
      options.javaHomeOpt.filter(_.nonEmpty),
      options.jvmIdOpt,
      options.scalaVersion,
      options.scalaBinaryVersion,
      options.scalaJsOptions.map(_.compilerPlugins).getOrElse(Nil) ++
        options.scalaNativeOptions.map(_.compilerPlugins).getOrElse(Nil) ++
        extraPlugins,
      allDependencies,
      options.stubsJarOpt.toSeq ++ options.testRunnerJarsOpt.getOrElse(Nil),
      addStubs = options.addStubsDependency,
      addJvmRunner = options.addRunnerDependency,
      addJvmTestRunner = options.scalaJsOptions.isEmpty && options.scalaNativeOptions.isEmpty && options.addTestRunnerDependency,
      addJsTestBridge = if (options.addTestRunnerDependency) options.scalaJsOptions.map(_.version) else None,
      addJmhDependencies = options.addJmhDependencies
    )

    val pluginScalacOptions = artifacts.compilerPlugins.map {
      case (_, _, path) =>
        s"-Xplugin:${path.toAbsolutePath}"
    }

    val extraScalacOptions =
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

    val scalaCompiler = ScalaCompiler(
            scalaVersion = options.scalaVersion,
      scalaBinaryVersion = options.scalaBinaryVersion,
           scalacOptions = Seq("-encoding", "UTF-8", "-deprecation", "-feature") ++ pluginScalacOptions ++ extraScalacOptions,
       compilerClassPath = artifacts.compilerClassPath
    )

    val project = Project(
               workspace = inputs.workspace,
                javaHome = artifacts.javaHome,
           scalaCompiler = scalaCompiler,
          scalaJsOptions = options.scalaJsOptions.map(_.config),
      scalaNativeOptions = options.scalaNativeOptions.map(_.config),
             projectName = inputs.projectName,
               classPath = artifacts.classPath,
                 sources = allScalaSources,
            resourceDirs = sources.resourceDirs
    )

    project.writeBloopFile()

    val outputPath = Bloop.compile(
      inputs.workspace,
      inputs.projectName,
      logger
    )

    Build(inputs, options, sources, artifacts, project, outputPath)
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
    onChange: => Unit
  ) {
    def newWatcher(): PathWatcher[PathWatchers.Event] = {
      val w = PathWatchers.get(true)
      watchers += w
      w
    }
    def dispose(): Unit = {
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
      watcher.register(os.FilePath(elem.path).resolveFrom(inputs.cwd).toNIO, depth) match {
        case l: com.swoval.functional.Either.Left[IOException, JBoolean] => throw l.getValue
        case _ =>
      }
    }

  def jmhBuild(inputs: Inputs, build: Build, logger: Logger, cwd: os.Path) = {
    val jmhProjectName = inputs.projectName + "_jmh"
    val jmhOutputDir = inputs.workspace / ".scala" / ".bloop" / jmhProjectName
    os.remove.all(jmhOutputDir)
    val jmhSourceDir = jmhOutputDir / "sources"
    val jmhResourceDir = jmhOutputDir / "resources"
    val retCode = Runner.run(
      build.artifacts.javaHome.toIO,
      build.fullClassPath.map(_.toFile),
      "org.openjdk.jmh.generators.bytecode.JmhBytecodeGenerator",
      Seq(build.output.toString, jmhSourceDir.toString, jmhResourceDir.toString, "default"),
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
          Inputs.Directory(jmhSourceDir.toString),
          Inputs.ResourceDirectory(jmhResourceDir.toString)
        )
      )
      val jmhBuild = Build.build(jmhInputs, build.options.copy(runJmh = false), logger, cwd)
      val updatedInputs = jmhInputs
      val updatedBuild = jmhBuild
      val forcedMainClassOpt = Some("org.openjdk.jmh.Main")
      Some(updatedBuild)
    }
    else None
  }

}
