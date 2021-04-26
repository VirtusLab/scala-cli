package scala.cli

import ammonite.compiler.iface.CodeWrapper
import com.swoval.files.FileTreeViews.Observer
import com.swoval.files.{FileTreeRepositories, PathWatcher, PathWatchers}
import scala.cli.internal.Util

import java.io.IOException
import java.lang.{Boolean => JBoolean}
import java.nio.file.{Path, Paths}
import java.util.concurrent.{ScheduledExecutorService, ScheduledFuture}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.scalanative.{build => sn}
import scala.util.control.NonFatal

final case class Build(
  sources: Sources,
  artifacts: Artifacts,
  project: Project,
  output: Path
) {
  def fullClassPath: Seq[Path] =
    output +: artifacts.classPath
  def foundMainClasses(): Seq[String] =
    MainClass.find(os.Path(output))
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
    projectName: String = "project",
    stubsJarOpt: Option[Path] = None,
    addStubsDependencyOpt: Option[Boolean] = None
  ) {
    def generatedSrcRoot(root: os.Path) = generatedSrcRootOpt.getOrElse {
      root / ".scala" / ".bloop" / projectName / ".src_generated"
    }
    def addStubsDependency: Boolean =
      addStubsDependencyOpt.getOrElse(stubsJarOpt.isEmpty)
  }

  def build(
    inputs: Inputs,
    options: Options,
    logger: Logger
  ): Build = {
    val sources = Sources(inputs.root, inputs, options.codeWrapper, options.scalaVersion)
    val maybePlatformSuffix =
      options.scalaJsOptions.map(_.platformSuffix)
        .orElse(options.scalaNativeOptions.map(_.platformSuffix))
    val finalSources = maybePlatformSuffix.fold(sources) { platformSuffix =>
      sources.checkPlatformDependencies(platformSuffix, options.scalaVersion, options.scalaBinaryVersion)
    }
    buildOnce(inputs.root, finalSources, options, logger)
  }

  def watch(
    inputs: Inputs,
    options: Options,
    logger: Logger,
    postAction: () => Unit = () => ()
  )(action: Build => Unit): Watcher = {

    def run() = {
      try {
        val build0 = build(inputs, options, logger)
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
          case d: Inputs.Directory => Int.MaxValue
          case s: Inputs.Script => -1
          case f: Inputs.ScalaFile => -1
        }
        val eventFilter: PathWatchers.Event => Boolean = elem match {
          case d: Inputs.Directory =>
            val dir = os.Path(Paths.get(d.path).toAbsolutePath)

            // Filtering event for directories, to ignore those related to the .bloop directory in particular
            event =>
              val p = os.Path(event.getTypedPath.getPath.toAbsolutePath)
              val relPath = p.relativeTo(dir)
              val isHidden = relPath.segments.exists(_.startsWith("."))
              def isScalaFile = relPath.last.endsWith(".sc") || relPath.last.endsWith(".scala")
              !isHidden && isScalaFile
          case s: Inputs.Script => _ => true
          case f: Inputs.ScalaFile => _ => true
        }

        val watcher0 = watcher.newWatcher()
        watcher0.register(Paths.get(elem.path), depth)
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
    root: os.Path,
    sources: Sources,
    options: Options,
    logger: Logger
  ): Build = {

    val generatedSources =
      sources.paths.map(p => root / p.split('/').toSeq) ++
        sources.generateSources(options.generatedSrcRoot(root))

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

    val artifacts = Artifacts(
      options.javaHomeOpt.filter(_.nonEmpty),
      options.jvmIdOpt,
      options.scalaVersion,
      options.scalaBinaryVersion,
      options.scalaJsOptions.map(_.compilerPlugins).getOrElse(Nil) ++
        options.scalaNativeOptions.map(_.compilerPlugins).getOrElse(Nil),
      allDependencies,
      options.stubsJarOpt.toSeq,
      addStubs = options.addStubsDependency
    )

    val pluginScalacOptions = artifacts.compilerPlugins.map {
      case (_, _, path) =>
        s"-Xplugin:${path.toAbsolutePath}"
    }
    val scalaCompiler = ScalaCompiler(
            scalaVersion = options.scalaVersion,
      scalaBinaryVersion = options.scalaBinaryVersion,
           scalacOptions = Seq("-encoding", "UTF-8", "-deprecation", "-feature") ++ pluginScalacOptions,
       compilerClassPath = artifacts.compilerClassPath
    )

    val project = Project(
               workspace = root,
                javaHome = artifacts.javaHome,
           scalaCompiler = scalaCompiler,
          scalaJsOptions = options.scalaJsOptions.map(_.config),
      scalaNativeOptions = options.scalaNativeOptions.map(_.config),
             projectName = options.projectName,
               classPath = artifacts.classPath,
                 sources = generatedSources
    )

    project.writeBloopFile()

    val outputPath = Bloop.compile(
      root.toNIO,
      options.projectName,
      logger
    )

    Build(sources, artifacts, project, outputPath)
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
        case _ => -1
      }
      watcher.register(Paths.get(elem.path), depth) match {
        case l: com.swoval.functional.Either.Left[IOException, JBoolean] => throw l.getValue
        case _ =>
      }
    }
}
