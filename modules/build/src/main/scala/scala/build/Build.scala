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
import scala.build.errors._
import scala.build.internal.{Constants, CustomCodeWrapper, MainClass, Util}
import scala.build.options.validation.ValidationException
import scala.build.options.{BuildOptions, ClassPathOptions, Platform, SNNumeralVersion, Scope}
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
    diagnostics: Option[Seq[(Either[String, os.Path], bsp4j.Diagnostic)]],
    generatedSources: Seq[GeneratedSource],
    isPartial: Boolean
  ) extends Build {
    def success: Boolean               = true
    def successfulOpt: Some[this.type] = Some(this)
    def outputOpt: Some[os.Path]       = Some(output)
    def fullClassPath: Seq[Path] =
      Seq(output.toNIO) ++ sources.resourceDirs.map(_.toNIO) ++ artifacts.classPath.map(_.toNIO)
    def foundMainClasses(): Seq[String] =
      MainClass.find(output)
    def retainedMainClass: Either[MainClassError, String] = {
      lazy val foundMainClasses0 = foundMainClasses()
      val defaultMainClassOpt = sources.mainClass
        .filter(name => foundMainClasses0.contains(name))
      def foundMainClass =
        if (foundMainClasses0.isEmpty) Left(new NoMainClassFoundError)
        else if (foundMainClasses0.length == 1) Right(foundMainClasses0.head)
        else
          Left(
            new SeveralMainClassesFoundError(
              ::(foundMainClasses0.head, foundMainClasses0.tail.toList),
              Nil
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
          options.platform.value
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

  def defaultStrictBloopJsonCheck = true

  def updateInputs(
    inputs: Inputs,
    options: BuildOptions,
    testOptions: Option[BuildOptions] = None
  ): Inputs = {

    // If some options are manually overridden, append a hash of the options to the project name
    // Using options, not options0 - only the command-line options are taken into account. No hash is
    // appended for options from the sources.
    val optionsHash     = options.hash
    val testOptionsHash = testOptions.flatMap(_.hash)

    inputs.copy(
      baseProjectName = inputs.baseProjectName
        + optionsHash.map("_" + _).getOrElse("")
        + testOptionsHash.map("_" + _).getOrElse("")
    )
  }

  private def build(
    inputs: Inputs,
    options: BuildOptions,
    logger: Logger,
    buildClient: BloopBuildClient,
    bloopServer: bloop.BloopServer,
    crossBuilds: Boolean,
    buildTests: Boolean,
    partial: Option[Boolean]
  ): Either[BuildException, Builds] = either {

    val crossSources = value {
      CrossSources.forInputs(
        inputs,
        Sources.defaultPreprocessors(
          options.scriptOptions.codeWrapper.getOrElse(CustomCodeWrapper)
        ),
        logger
      )
    }
    val sharedOptions = crossSources.sharedOptions(options)
    val crossOptions  = sharedOptions.crossOptions

    def doPostProcess(build: Build, inputs: Inputs, scope: Scope): Unit = build match {
      case build: Build.Successful =>
        postProcess(
          build.generatedSources,
          inputs.generatedSrcRoot(scope),
          build.output,
          logger,
          inputs.workspace,
          updateSemanticDbs = true,
          scalaVersion = build.project.scalaCompiler.scalaVersion
        ).left.foreach(_.foreach(logger.message(_)))
      case _ =>
    }

    def doBuild(
      overrideOptions: BuildOptions
    ): Either[BuildException, (Build, Option[Build])] = either {

      val baseOptions   = overrideOptions.orElse(sharedOptions)
      val scopedSources = value(crossSources.scopedSources(baseOptions))

      val mainSources = scopedSources.sources(Scope.Main, baseOptions)
      val mainOptions = mainSources.buildOptions

      val testSources = scopedSources.sources(Scope.Test, baseOptions)
      val testOptions = testSources.buildOptions

      val inputs0 = updateInputs(
        inputs,
        mainOptions, // update hash in inputs with options coming from the CLI or cross-building, not from the sources
        Some(testOptions)
      )

      def doBuildScope(
        options: BuildOptions,
        sources: Sources,
        scope: Scope
      ): Either[BuildException, Build] =
        either {
          val sources0 = sources.withVirtualDir(inputs0, scope, options)

          val generatedSources = sources0.generateSources(inputs0.generatedSrcRoot(scope))

          val scopeParams =
            if (scope == Scope.Main) Nil
            else Seq(scope.name)

          buildClient.setProjectParams(scopeParams ++ value(options.projectParams))

          val res = build(
            inputs0,
            sources0,
            generatedSources,
            options,
            scope,
            logger,
            buildClient,
            bloopServer,
            buildTests,
            partial
          )

          value(res)
        }

      val mainBuild = value(doBuildScope(mainOptions, mainSources, Scope.Main))

      val testBuildOpt =
        if (buildTests) {
          val testBuild = value {
            mainBuild match {
              case s: Build.Successful =>
                val extraTestOptions = BuildOptions(
                  classPathOptions = ClassPathOptions(
                    extraClassPath = Seq(s.output)
                  )
                )
                val testOptions0 = extraTestOptions.orElse(testOptions)
                doBuildScope(testOptions0, testSources, Scope.Test)
              case _ =>
                Right(Build.Cancelled(
                  inputs,
                  sharedOptions,
                  Scope.Test,
                  "Parent build failed or cancelled"
                ))
            }
          }
          Some(testBuild)
        }
        else None

      doPostProcess(mainBuild, inputs0, Scope.Main)
      for (testBuild <- testBuildOpt)
        doPostProcess(testBuild, inputs0, Scope.Test)

      (mainBuild, testBuildOpt)
    }

    def buildScopes(): Either[BuildException, (Build, Seq[Build], Option[Build], Seq[Build])] =
      either {
        val (mainBuild, testBuild) = value(doBuild(BuildOptions()))

        val (extraMainBuilds: Seq[Build], extraTestBuilds: Seq[Build]) =
          if (crossBuilds) {
            val extraBuilds = value {
              val maybeBuilds = crossOptions.map(doBuild)

              maybeBuilds
                .sequence
                .left.map(CompositeBuildException(_))
            }
            (extraBuilds.map(_._1), extraBuilds.flatMap(_._2))
          }
          else
            (Nil, Nil)

        (mainBuild, extraMainBuilds, testBuild, extraTestBuilds)
      }

    val (mainBuild, extraBuilds, testBuildOpt, extraTestBuilds) = value(buildScopes())

    copyResourceToClassesDir(mainBuild)
    for (testBuild <- testBuildOpt)
      copyResourceToClassesDir(testBuild)

    Builds(Seq(mainBuild) ++ testBuildOpt.toSeq, Seq(extraBuilds, extraTestBuilds))
  }

  private def copyResourceToClassesDir(build: Build) = build match {
    case b: Build.Successful =>
      for {
        resourceDirPath  <- b.sources.resourceDirs.filter(os.exists(_))
        resourceFilePath <- os.walk(resourceDirPath).filter(os.isFile(_))
        relativeResourcePath = resourceFilePath.relativeTo(resourceDirPath)
        // dismiss files generated by scala-cli
        if !relativeResourcePath.startsWith(os.rel / Constants.workspaceDirName)
      } {
        val destPath = b.output / relativeResourcePath
        os.copy(
          resourceFilePath,
          destPath,
          replaceExisting = true,
          createFolders = true
        )
      }
    case _ =>
  }

  private def build(
    inputs: Inputs,
    sources: Sources,
    generatedSources: Seq[GeneratedSource],
    options: BuildOptions,
    scope: Scope,
    logger: Logger,
    buildClient: BloopBuildClient,
    bloopServer: bloop.BloopServer,
    buildTests: Boolean,
    partial: Option[Boolean]
  ): Either[BuildException, Build] = either {

    val build0 = value {
      buildOnce(
        inputs,
        sources,
        generatedSources,
        options,
        scope,
        logger,
        buildClient,
        bloopServer,
        partial
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
              successful.options.javaHome().value.javaCommand,
              buildClient,
              bloopServer,
              buildTests
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
    root / Constants.workspaceDirName / projectName / "classes"
  def classesDir(root: os.Path, projectName: String, scope: Scope): os.Path =
    classesRootDir(root, projectName) / scope.name

  def scalaNativeSupported(
    options: BuildOptions,
    inputs: Inputs
  ): Either[BuildException, Option[ScalaNativeCompatibilityError]] =
    either {
      val scalaVersion  = value(options.scalaParams).scalaVersion
      val nativeVersion = options.scalaNativeOptions.numeralVersion
      val isCompatible = nativeVersion match {
        case Some(snNumeralVer) =>
          if (snNumeralVer < SNNumeralVersion(0, 4, 1) && Properties.isWin)
            false
          else if (scalaVersion.startsWith("3.0"))
            false
          else if (scalaVersion.startsWith("3"))
            snNumeralVer >= SNNumeralVersion(0, 4, 3)
          else if (scalaVersion.startsWith("2.13"))
            true
          else if (scalaVersion.startsWith("2.12"))
            inputs.sourceFiles().forall {
              case _: Inputs.AnyScript => false
              case _                   => true
            }
          else false
        case None => false
      }
      if (isCompatible) None
      else
        Some(
          new ScalaNativeCompatibilityError(
            scalaVersion,
            options.scalaNativeOptions.finalVersion
          )
        )
    }

  def build(
    inputs: Inputs,
    options: BuildOptions,
    threads: BuildThreads,
    bloopConfig: BloopRifleConfig,
    logger: Logger,
    crossBuilds: Boolean,
    buildTests: Boolean,
    partial: Option[Boolean]
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
      (inputs.workspace / Constants.workspaceDirName).toNIO,
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
        crossBuilds = crossBuilds,
        buildTests = buildTests,
        partial = partial
      )
    }
  }

  def build(
    inputs: Inputs,
    options: BuildOptions,
    bloopConfig: BloopRifleConfig,
    logger: Logger,
    crossBuilds: Boolean,
    buildTests: Boolean,
    partial: Option[Boolean]
  ): Either[BuildException, Builds] =
    build(
      inputs,
      options, /*scope,*/ BuildThreads.create(),
      bloopConfig,
      logger,
      crossBuilds = crossBuilds,
      buildTests = buildTests,
      partial = partial
    )

  def validate(
    logger: Logger,
    options: BuildOptions
  ): Either[BuildException, Unit] = {
    val (errors, otherDiagnostics) = options.validate.toSeq.partition(_.severity == Severity.Error)
    logger.log(otherDiagnostics)
    if (errors.nonEmpty)
      Left(CompositeBuildException(errors.map(new ValidationException(_))))
    else
      Right(())
  }

  def watch(
    inputs: Inputs,
    options: BuildOptions,
    bloopConfig: BloopRifleConfig,
    logger: Logger,
    crossBuilds: Boolean,
    buildTests: Boolean,
    partial: Option[Boolean],
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
      (inputs.workspace / Constants.workspaceDirName).toNIO,
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
          crossBuilds = crossBuilds,
          buildTests = buildTests,
          partial = partial
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

  def releaseFlag(
    options: BuildOptions,
    logger: Logger
  ): Option[Int] = {
    val bloopJvmV = options.javaOptions.bloopJvmVersion
    val javaHome  = options.javaHome()
    if (bloopJvmV.exists(javaHome.value.version > _.value)) {
      logger.log(List(Diagnostic(
        Diagnostic.Messages.bloopTooOld,
        Severity.Warning,
        javaHome.positions ++ bloopJvmV.map(_.positions).getOrElse(Nil)
      )))
      None
    }
    else if (options.javaOptions.bloopJvmVersion.exists(_.value == 8))
      None
    else if (options.scalaOptions.scalacOptions.toSeq.exists(_.value.value == "-release"))
      None
    else
      Some(javaHome.value.version)
  }

  def buildProject(
    inputs: Inputs,
    sources: Sources,
    generatedSources: Seq[GeneratedSource],
    options: BuildOptions,
    scope: Scope,
    logger: Logger
  ): Either[BuildException, Project] = either {

    val params     = value(options.scalaParams)
    val allSources = sources.paths.map(_._1) ++ generatedSources.map(_.generated)

    val classesDir0 = classesDir(inputs.workspace, inputs.projectName, scope)

    val artifacts = value(options.artifacts(logger))

    val pluginScalacOptions = artifacts.compilerPlugins.distinct.map {
      case (_, _, path) =>
        s"-Xplugin:$path"
    }

    val generateSemanticDbs = options.scalaOptions.generateSemanticDbs.getOrElse(false)

    val semanticDbScalacOptions =
      if (generateSemanticDbs)
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

    val semanticDbJavacOptions =
      // FIXME Should this be in scalaOptions, now that we use it for javac stuff too?
      if (generateSemanticDbs) {
        // from https://github.com/scalameta/metals/blob/04405c0401121b372ea1971c361e05108fb36193/metals/src/main/scala/scala/meta/internal/metals/JavaInteractiveSemanticdb.scala#L137-L146
        val compilerPackages = Seq(
          "com.sun.tools.javac.api",
          "com.sun.tools.javac.code",
          "com.sun.tools.javac.model",
          "com.sun.tools.javac.tree",
          "com.sun.tools.javac.util"
        )
        val exports = compilerPackages.flatMap { pkg =>
          Seq("-J--add-exports", s"-Jjdk.compiler/$pkg=ALL-UNNAMED")
        }

        Seq(
          // does the path need to be escaped somehow?
          s"-Xplugin:semanticdb -sourceroot:${inputs.workspace} -targetroot:javac-classes-directory"
        ) ++ exports
      }
      else
        Nil

    val sourceRootScalacOptions =
      if (params.scalaVersion.startsWith("2.")) Nil
      else Seq("-sourceroot", inputs.workspace.toString)

    val scalaJsScalacOptions =
      if (options.platform.value == Platform.JS && !params.scalaVersion.startsWith("2."))
        Seq("-scalajs")
      else Nil

    val releaseFlagVersion = releaseFlag(options, logger).map(_.toString)

    val scalacReleaseV = releaseFlagVersion.map(v => List("-release", v)).getOrElse(Nil)
    val javacReleaseV  = releaseFlagVersion.map(v => List("--release", v)).getOrElse(Nil)

    val scalacOptions =
      options.scalaOptions.scalacOptions.toSeq.map(_.value.value) ++
        pluginScalacOptions ++
        semanticDbScalacOptions ++
        sourceRootScalacOptions ++
        scalaJsScalacOptions ++
        scalacReleaseV

    val scalaCompiler = ScalaCompiler(
      scalaVersion = params.scalaVersion,
      scalaBinaryVersion = params.scalaBinaryVersion,
      scalacOptions = scalacOptions,
      compilerClassPath = artifacts.compilerClassPath
    )

    val javacOptions = javacReleaseV ++ semanticDbJavacOptions ++ options.javaOptions.javacOptions

    // `test` scope should contains class path to main scope
    val mainClassesPath =
      if (scope == Scope.Test)
        List(classesDir(inputs.workspace, inputs.projectName, Scope.Main))
      else Nil

    value(validate(logger, options))

    val fullClassPath = artifacts.compileClassPath ++
      mainClassesPath ++
      artifacts.javacPluginDependencies.map(_._3) ++
      artifacts.extraJavacPlugins

    val project = Project(
      directory = inputs.workspace / Constants.workspaceDirName,
      workspace = inputs.workspace,
      classesDir = classesDir0,
      scalaCompiler = scalaCompiler,
      scalaJsOptions =
        if (options.platform.value == Platform.JS) Some(options.scalaJsOptions.config(logger))
        else None,
      scalaNativeOptions =
        if (options.platform.value == Platform.Native)
          Some(options.scalaNativeOptions.bloopConfig())
        else None,
      projectName = inputs.scopeProjectName(scope),
      classPath = fullClassPath,
      resolution = Some(Project.resolution(artifacts.detailedArtifacts)),
      sources = allSources,
      resourceDirs = sources.resourceDirs,
      scope = scope,
      javaHomeOpt = Option(options.javaHomeLocation().value),
      javacOptions = javacOptions
    )
    project
  }

  def prepareBuild(
    inputs: Inputs,
    sources: Sources,
    generatedSources: Seq[GeneratedSource],
    options: BuildOptions,
    scope: Scope,
    logger: Logger,
    buildClient: BloopBuildClient
  ): Either[BuildException, (os.Path, ScalaParameters, Artifacts, Project, Boolean)] = either {

    val params = value(options.scalaParams)

    val classesDir0 = classesDir(inputs.workspace, inputs.projectName, scope)

    val artifacts = value(options.artifacts(logger))

    value(validate(logger, options))

    val project = value(buildProject(inputs, sources, generatedSources, options, scope, logger))

    val updatedBloopConfig = project.writeBloopFile(
      options.internal.strictBloopJsonCheck.getOrElse(defaultStrictBloopJsonCheck),
      logger
    )

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
    buildClient.setGeneratedSources(scope, generatedSources)

    (classesDir0, params, artifacts, project, updatedBloopConfig)
  }

  def buildOnce(
    inputs: Inputs,
    sources: Sources,
    generatedSources: Seq[GeneratedSource],
    options0: BuildOptions,
    scope: Scope,
    logger: Logger,
    buildClient: BloopBuildClient,
    bloopServer: bloop.BloopServer,
    partialOpt: Option[Boolean]
  ): Either[BuildException, Build] = either {
    val options = options0.copy(javaOptions =
      options0.javaOptions.copy(bloopJvmVersion =
        Some(Positioned[Int](
          List(Position.Bloop(bloopServer.bloopInfo.javaHome)),
          bloopServer.bloopInfo.jvmVersion
        ))
      )
    )

    if (options.platform.value == Platform.Native)
      value(scalaNativeSupported(options, inputs)) match {
        case None        =>
        case Some(error) => value(Left(error))
      }

    val (classesDir0, scalaParams, artifacts, project, updatedBloopConfig) = value {
      prepareBuild(
        inputs,
        sources,
        generatedSources,
        options,
        scope,
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
    buildClient.setGeneratedSources(scope, generatedSources)

    val partial = partialOpt.getOrElse {
      options.notForBloopOptions.packageOptions.packageTypeOpt.exists(_.sourceBased)
    }

    val success =
      partial ||
      Bloop.compile(
        inputs.scopeProjectName(scope),
        bloopServer,
        logger,
        buildTargetsTimeout = 20.seconds
      )

    if (success)
      Successful(
        inputs,
        options,
        scalaParams,
        scope,
        sources,
        artifacts,
        project,
        classesDir0,
        buildClient.diagnostics,
        generatedSources,
        partial
      )
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
  ): Either[Seq[String], Unit] =
    if (os.exists(classesDir)) {

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
    else
      Right(())

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
    bloopServer: bloop.BloopServer,
    buildTests: Boolean
  ): Either[BuildException, Option[Build]] = either {
    val jmhProjectName = inputs.projectName + "_jmh"
    val jmhOutputDir   = inputs.workspace / Constants.workspaceDirName / jmhProjectName
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
          crossBuilds = false,
          buildTests = buildTests,
          partial = None
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
