package scala.build

import ch.epfl.scala.bsp4j
import com.swoval.files.FileTreeViews.Observer
import com.swoval.files.{PathWatcher, PathWatchers}
import dependency.ScalaParameters

import java.io.File
import java.nio.file.FileSystemException
import java.util.concurrent.{ScheduledExecutorService, ScheduledFuture}

import scala.annotation.tailrec
import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.actionable.ActionablePreprocessor
import scala.build.compiler.{ScalaCompiler, ScalaCompilerMaker}
import scala.build.errors.*
import scala.build.input.VirtualScript.VirtualScriptNameRegex
import scala.build.input.*
import scala.build.internal.resource.ResourceMapper
import scala.build.internal.{Constants, CustomCodeWrapper, MainClass, Util}
import scala.build.options.ScalaVersionUtil.asVersion
import scala.build.options.*
import scala.build.options.validation.ValidationException
import scala.build.postprocessing.*
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt
import scala.util.control.NonFatal
import scala.util.{Properties, Try}

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
    scalaParams: Option[ScalaParameters],
    scope: Scope,
    sources: Sources,
    artifacts: Artifacts,
    project: Project,
    output: os.Path,
    diagnostics: Option[Seq[(Either[String, os.Path], bsp4j.Diagnostic)]],
    generatedSources: Seq[GeneratedSource],
    isPartial: Boolean
  ) extends Build {
    def success: Boolean                  = true
    def successfulOpt: Some[this.type]    = Some(this)
    def outputOpt: Some[os.Path]          = Some(output)
    def dependencyClassPath: Seq[os.Path] = sources.resourceDirs ++ artifacts.classPath
    def fullClassPath: Seq[os.Path]       = Seq(output) ++ dependencyClassPath
    def foundMainClasses(): Seq[String] =
      MainClass.find(output) ++ options.classPathOptions.extraClassPath.flatMap(MainClass.find)
    def retainedMainClass(
      mainClasses: Seq[String],
      commandString: String,
      logger: Logger
    ): Either[BuildException, String] = {
      val defaultMainClassOpt = sources.defaultMainClass
        .filter(name => mainClasses.contains(name))
      def foundMainClass: Either[BuildException, String] =
        mainClasses match {
          case Seq()          => Left(new NoMainClassFoundError)
          case Seq(mainClass) => Right(mainClass)
          case _ =>
            inferredMainClass(mainClasses, logger)
              .left.flatMap { mainClasses =>
                options.interactive.flatMap { interactive =>
                  interactive
                    .chooseOne(
                      "Found several main classes. Which would you like to run?",
                      mainClasses.toList
                    )
                    .toRight {
                      SeveralMainClassesFoundError(
                        ::(mainClasses.head, mainClasses.tail.toList),
                        commandString,
                        Nil
                      )
                    }
                }
              }
        }

      defaultMainClassOpt match {
        case Some(cls) => Right(cls)
        case None      => foundMainClass
      }
    }
    private def inferredMainClass(
      mainClasses: Seq[String],
      logger: Logger
    ): Either[Seq[String], String] = {
      val scriptInferredMainClasses =
        sources.inMemory.map(im => im.originalPath.map(_._1))
          .flatMap {
            case Right(originalRelPath) if originalRelPath.toString.endsWith(".sc") =>
              Some {
                originalRelPath
                  .toString
                  .replace(".", "_")
                  .replace("/", ".")
              }
            case Left(VirtualScriptNameRegex(name)) => Some(s"${name}_sc")
            case _                                  => None
          }
      val filteredMainClasses =
        mainClasses.filter(!scriptInferredMainClasses.contains(_))
      if (filteredMainClasses.length == 1) {
        val pickedMainClass = filteredMainClasses.head
        if (scriptInferredMainClasses.nonEmpty) {
          val firstScript   = scriptInferredMainClasses.head
          val scriptsString = scriptInferredMainClasses.mkString(", ")
          logger.message(
            s"Running $pickedMainClass. Also detected script main classes: $scriptsString"
          )
          logger.message(
            s"You can run any one of them by passing option --main-class, i.e. --main-class $firstScript"
          )
          logger.message(
            "All available main classes can always be listed by passing option --list-main-classes"
          )
        }
        Right(pickedMainClass)
      }
      else
        Left(mainClasses)
    }
    def retainedMainClassOpt(
      mainClasses: Seq[String],
      logger: Logger
    ): Option[String] = {
      val defaultMainClassOpt = sources.defaultMainClass
        .filter(name => mainClasses.contains(name))
      def foundMainClass =
        mainClasses match {
          case Seq()          => None
          case Seq(mainClass) => Some(mainClass)
          case _              => inferredMainClass(mainClasses, logger).toOption
        }

      defaultMainClassOpt.orElse(foundMainClass)
    }

    def crossKey: CrossKey = {
      val optKey = scalaParams.map { params =>
        BuildOptions.CrossKey(
          params.scalaVersion,
          options.platform.value
        )
      }
      CrossKey(optKey, scope)
    }
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
    options: BuildOptions,
    testOptions: Option[BuildOptions] = None
  ): Inputs = {

    // If some options are manually overridden, append a hash of the options to the project name
    // Using options, not options0 - only the command-line options are taken into account. No hash is
    // appended for options from the sources.
    val optionsHash     = options.hash
    val testOptionsHash = testOptions.flatMap(_.hash)

    inputs.copy(
      baseProjectName =
        inputs.baseProjectName
          + optionsHash.map("_" + _).getOrElse("")
          + testOptionsHash.map("_" + _).getOrElse("")
    )
  }

  private def build(
    inputs: Inputs,
    options: BuildOptions,
    logger: Logger,
    buildClient: BloopBuildClient,
    compiler: ScalaCompiler,
    docCompilerOpt: Option[ScalaCompiler],
    crossBuilds: Boolean,
    buildTests: Boolean,
    partial: Option[Boolean],
    actionableDiagnostics: Option[Boolean]
  ): Either[BuildException, Builds] = either {
    // allInputs contains elements from using directives
    val (crossSources, allInputs) = value {
      CrossSources.forInputs(
        inputs,
        Sources.defaultPreprocessors(
          options.scriptOptions.codeWrapper.getOrElse(CustomCodeWrapper),
          options.archiveCache,
          options.internal.javaClassNameVersionOpt,
          () => options.javaHome().value.javaCommand
        ),
        logger
      )
    }
    val sharedOptions = crossSources.sharedOptions(options)
    val crossOptions  = sharedOptions.crossOptions

    def doPostProcess(build: Build, inputs: Inputs, scope: Scope): Unit = build match {
      case build: Build.Successful =>
        for (sv <- build.project.scalaCompiler.map(_.scalaVersion))
          postProcess(
            build.generatedSources,
            inputs.generatedSrcRoot(scope),
            build.output,
            logger,
            inputs.workspace,
            updateSemanticDbs = true,
            scalaVersion = sv
          ).left.foreach(_.foreach(logger.message(_)))
      case _ =>
    }

    final case class NonCrossBuilds(
      main: Build,
      testOpt: Option[Build],
      docOpt: Option[Build],
      testDocOpt: Option[Build]
    )

    def doBuild(
      overrideOptions: BuildOptions
    ): Either[BuildException, NonCrossBuilds] = either {

      val baseOptions   = overrideOptions.orElse(sharedOptions)
      val scopedSources = value(crossSources.scopedSources(baseOptions))

      val mainSources = scopedSources.sources(Scope.Main, baseOptions)
      val mainOptions = mainSources.buildOptions

      val testSources = scopedSources.sources(Scope.Test, baseOptions)
      val testOptions = testSources.buildOptions

      val inputs0 = updateInputs(
        allInputs,
        mainOptions, // update hash in inputs with options coming from the CLI or cross-building, not from the sources
        Some(testOptions).filter(_ != mainOptions)
      )

      def doBuildScope(
        options: BuildOptions,
        sources: Sources,
        scope: Scope,
        actualCompiler: ScalaCompiler = compiler
      ): Either[BuildException, Build] =
        either {
          val sources0 = sources.withVirtualDir(inputs0, scope, options)

          val generatedSources = sources0.generateSources(inputs0.generatedSrcRoot(scope))

          val res = build(
            inputs0,
            sources0,
            generatedSources,
            options,
            scope,
            logger,
            buildClient,
            actualCompiler,
            buildTests,
            partial,
            actionableDiagnostics
          )

          value(res)
        }

      val mainBuild = value(doBuildScope(mainOptions, mainSources, Scope.Main))
      val mainDocBuildOpt = docCompilerOpt match {
        case None => None
        case Some(docCompiler) =>
          Some(value(doBuildScope(
            mainOptions,
            mainSources,
            Scope.Main,
            actualCompiler = docCompiler
          )))
      }

      def testBuildOpt(doc: Boolean = false): Either[BuildException, Option[Build]] = either {
        if (buildTests) {
          val actualCompilerOpt =
            if (doc) docCompilerOpt
            else Some(compiler)
          actualCompilerOpt match {
            case None => None
            case Some(actualCompiler) =>
              val testBuild = value {
                mainBuild match {
                  case s: Build.Successful =>
                    val extraTestOptions = BuildOptions(
                      classPathOptions = ClassPathOptions(
                        extraClassPath = Seq(s.output)
                      )
                    )
                    val testOptions0 = extraTestOptions.orElse(testOptions)
                    doBuildScope(
                      testOptions0,
                      testSources,
                      Scope.Test,
                      actualCompiler = actualCompiler
                    )
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
        }
        else None
      }

      val testBuildOpt0 = value(testBuildOpt())
      doPostProcess(mainBuild, inputs0, Scope.Main)
      for (testBuild <- testBuildOpt0)
        doPostProcess(testBuild, inputs0, Scope.Test)

      val docTestBuildOpt0 = value(testBuildOpt(doc = true))

      NonCrossBuilds(mainBuild, testBuildOpt0, mainDocBuildOpt, docTestBuildOpt0)
    }

    def buildScopes(): Either[BuildException, Builds] =
      either {
        val nonCrossBuilds = value(doBuild(BuildOptions()))

        val (extraMainBuilds, extraTestBuilds, extraDocBuilds, extraDocTestBuilds) =
          if (crossBuilds) {
            val extraBuilds = value {
              val maybeBuilds = crossOptions.map(doBuild)

              maybeBuilds
                .sequence
                .left.map(CompositeBuildException(_))
            }
            (
              extraBuilds.map(_.main),
              extraBuilds.flatMap(_.testOpt),
              extraBuilds.flatMap(_.docOpt),
              extraBuilds.flatMap(_.testDocOpt)
            )
          }
          else
            (Nil, Nil, Nil, Nil)

        Builds(
          Seq(nonCrossBuilds.main) ++ nonCrossBuilds.testOpt.toSeq,
          Seq(extraMainBuilds, extraTestBuilds),
          nonCrossBuilds.docOpt.toSeq ++ nonCrossBuilds.testDocOpt.toSeq,
          Seq(extraDocBuilds, extraDocTestBuilds)
        )
      }

    val builds = value(buildScopes())

    ResourceMapper.copyResourceToClassesDir(builds.main)
    for (testBuild <- builds.get(Scope.Test))
      ResourceMapper.copyResourceToClassesDir(testBuild)

    if (actionableDiagnostics.getOrElse(false)) {
      val projectOptions = builds.get(Scope.Test).getOrElse(builds.main).options
      projectOptions.logActionableDiagnostics(logger)
    }

    builds
  }

  private def build(
    inputs: Inputs,
    sources: Sources,
    generatedSources: Seq[GeneratedSource],
    options: BuildOptions,
    scope: Scope,
    logger: Logger,
    buildClient: BloopBuildClient,
    compiler: ScalaCompiler,
    buildTests: Boolean,
    partial: Option[Boolean],
    actionableDiagnostics: Option[Boolean]
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
        compiler,
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
              compiler,
              buildTests,
              actionableDiagnostics = actionableDiagnostics
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
  def classesDir(root: os.Path, projectName: String, scope: Scope, suffix: String = ""): os.Path =
    classesRootDir(root, projectName) / s"${scope.name}$suffix"

  def resourcesRegistry(
    root: os.Path,
    projectName: String,
    scope: Scope
  ): os.Path =
    root / Constants.workspaceDirName / projectName / s"resources-${scope.name}"

  def scalaNativeSupported(
    options: BuildOptions,
    inputs: Inputs,
    logger: Logger
  ): Either[BuildException, Option[ScalaNativeCompatibilityError]] =
    either {
      val scalaParamsOpt = value(options.scalaParams)
      scalaParamsOpt.flatMap { scalaParams =>
        val scalaVersion       = scalaParams.scalaVersion
        val nativeVersionMaybe = options.scalaNativeOptions.numeralVersion
        def snCompatError =
          Left(
            new ScalaNativeCompatibilityError(
              scalaVersion,
              options.scalaNativeOptions.finalVersion
            )
          )
        def warnIncompatibleNativeOptions(numeralVersion: SNNumeralVersion) =
          if (
            numeralVersion < SNNumeralVersion(0, 4, 4)
            && options.scalaNativeOptions.embedResources.isDefined
          )
            logger.diagnostic(
              "This Scala Version cannot embed resources, regardless of the options used."
            )

        val numeralOrError: Either[ScalaNativeCompatibilityError, SNNumeralVersion] =
          nativeVersionMaybe match {
            case Some(snNumeralVer) =>
              if (snNumeralVer < SNNumeralVersion(0, 4, 1) && Properties.isWin)
                snCompatError
              else if (scalaVersion.startsWith("3.0"))
                snCompatError
              else if (scalaVersion.startsWith("3"))
                if (snNumeralVer >= SNNumeralVersion(0, 4, 3)) Right(snNumeralVer)
                else snCompatError
              else if (scalaVersion.startsWith("2.13"))
                Right(snNumeralVer)
              else if (scalaVersion.startsWith("2.12"))
                if (
                  inputs.sourceFiles().forall {
                    case _: AnyScript => snNumeralVer >= SNNumeralVersion(0, 4, 3)
                    case _            => true
                  }
                ) Right(snNumeralVer)
                else snCompatError
              else snCompatError
            case None => snCompatError
          }

        numeralOrError match {
          case Left(compatError) => Some(compatError)
          case Right(snNumeralVersion) =>
            warnIncompatibleNativeOptions(snNumeralVersion)
            None
        }
      }
    }

  def build(
    inputs: Inputs,
    options: BuildOptions,
    compilerMaker: ScalaCompilerMaker,
    docCompilerMakerOpt: Option[ScalaCompilerMaker],
    logger: Logger,
    crossBuilds: Boolean,
    buildTests: Boolean,
    partial: Option[Boolean],
    actionableDiagnostics: Option[Boolean]
  ): Either[BuildException, Builds] = {
    val buildClient = BloopBuildClient.create(
      logger,
      keepDiagnostics = options.internal.keepDiagnostics
    )
    val classesDir0 = classesRootDir(inputs.workspace, inputs.projectName)

    compilerMaker.withCompiler(
      inputs.workspace / Constants.workspaceDirName,
      classesDir0,
      buildClient,
      logger
    ) { compiler =>
      docCompilerMakerOpt match {
        case None =>
          build(
            inputs = inputs,
            options = options,
            logger = logger,
            buildClient = buildClient,
            compiler = compiler,
            docCompilerOpt = None,
            crossBuilds = crossBuilds,
            buildTests = buildTests,
            partial = partial,
            actionableDiagnostics = actionableDiagnostics
          )
        case Some(docCompilerMaker) =>
          docCompilerMaker.withCompiler(
            inputs.workspace / Constants.workspaceDirName,
            classesDir0, // ???
            buildClient,
            logger
          ) { docCompiler =>
            build(
              inputs = inputs,
              options = options,
              logger = logger,
              buildClient = buildClient,
              compiler = compiler,
              docCompilerOpt = Some(docCompiler),
              crossBuilds = crossBuilds,
              buildTests = buildTests,
              partial = partial,
              actionableDiagnostics = actionableDiagnostics
            )
          }
      }
    }
  }

  def validate(
    logger: Logger,
    options: BuildOptions
  ): Either[BuildException, Unit] = {
    val (errors, otherDiagnostics) = options.validate.partition(_.severity == Severity.Error)
    logger.log(otherDiagnostics)
    if (errors.nonEmpty)
      Left(CompositeBuildException(errors.map(new ValidationException(_))))
    else
      Right(())
  }

  def watch(
    inputs: Inputs,
    options: BuildOptions,
    compilerMaker: ScalaCompilerMaker,
    docCompilerMakerOpt: Option[ScalaCompilerMaker],
    logger: Logger,
    crossBuilds: Boolean,
    buildTests: Boolean,
    partial: Option[Boolean],
    actionableDiagnostics: Option[Boolean],
    postAction: () => Unit = () => ()
  )(action: Either[BuildException, Builds] => Unit): Watcher = {

    val buildClient = BloopBuildClient.create(
      logger,
      keepDiagnostics = options.internal.keepDiagnostics
    )
    val threads     = BuildThreads.create()
    val classesDir0 = classesRootDir(inputs.workspace, inputs.projectName)
    val compiler = compilerMaker.create(
      inputs.workspace / Constants.workspaceDirName,
      classesDir0,
      buildClient,
      logger
    )
    val docCompilerOpt = docCompilerMakerOpt.map(_.create(
      inputs.workspace / Constants.workspaceDirName,
      classesDir0,
      buildClient,
      logger
    ))

    var res: Either[BuildException, Builds] = null

    def run(): Unit = {
      try {
        res = build(
          inputs,
          options,
          logger,
          buildClient,
          compiler,
          docCompilerOpt,
          crossBuilds = crossBuilds,
          buildTests = buildTests,
          partial = partial,
          actionableDiagnostics = actionableDiagnostics
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

    val watcher = new Watcher(ListBuffer(), threads.fileWatcher, run(), compiler.shutdown())

    def doWatch(): Unit = {
      val elements: Seq[Element] =
        if (res == null) inputs.elements
        else
          res.map { builds =>
            val mainElems = builds.main.inputs.elements
            val testElems = builds.get(Scope.Test).map(_.inputs.elements).getOrElse(Nil)
            (mainElems ++ testElems).distinct
          }.getOrElse(inputs.elements)
      for (elem <- elements) {
        val depth = elem match {
          case _: SingleFile => -1
          case _             => Int.MaxValue
        }
        val eventFilter: PathWatchers.Event => Boolean = elem match {
          case d: Directory =>
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
          case d: OnDisk =>
            watcher0.register(d.path.toNIO, depth)
          case _: Virtual =>
        }
        watcher0.addObserver {
          onChangeBufferedObserver { event =>
            if (eventFilter(event))
              watcher.schedule()
          }
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
    compilerJvmVersionOpt: Option[Positioned[Int]],
    logger: Logger
  ): Option[Int] = {
    lazy val javaHome = options.javaHome()
    if (compilerJvmVersionOpt.exists(javaHome.value.version > _.value)) {
      logger.log(List(Diagnostic(
        Diagnostic.Messages.bloopTooOld,
        Severity.Warning,
        javaHome.positions ++ compilerJvmVersionOpt.map(_.positions).getOrElse(Nil)
      )))
      None
    }
    else if (compilerJvmVersionOpt.exists(_.value == 8))
      None
    else if (
      options.scalaOptions.scalacOptions.values.exists(
        _.headOption.exists(_.value.value == "-release")
      )
    )
      None
    else
      Some(javaHome.value.version)
  }

  /** Builds a Bloop project.
    *
    * @param inputs
    *   inputs to be included in the project
    * @param sources
    *   sources to be included in the project
    * @param generatedSources
    *   sources generated by Scala CLI as part of the build
    * @param options
    *   build options
    * @param compilerJvmVersionOpt
    *   compiler JVM version (optional)
    * @param scope
    *   build scope for which the project is to be created
    * @param logger
    *   logger
    * @param maybeRecoverOnError
    *   a function handling [[BuildException]] instances, possibly recovering them; returns None on
    *   recovery, Some(e: BuildException) otherwise
    * @return
    *   a bloop [[Project]]
    */
  def buildProject(
    inputs: Inputs,
    sources: Sources,
    generatedSources: Seq[GeneratedSource],
    options: BuildOptions,
    compilerJvmVersionOpt: Option[Positioned[Int]],
    scope: Scope,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e)
  ): Either[BuildException, Project] = either {

    val allSources = sources.paths.map(_._1) ++ generatedSources.map(_.generated)

    val classesDir0 = classesDir(inputs.workspace, inputs.projectName, scope)
    val scaladocDir = classesDir(inputs.workspace, inputs.projectName, scope, suffix = "-doc")

    val artifacts = value(options.artifacts(logger, scope, maybeRecoverOnError))

    val generateSemanticDbs = options.scalaOptions.generateSemanticDbs.getOrElse(false)

    val releaseFlagVersion = releaseFlag(options, compilerJvmVersionOpt, logger).map(_.toString)

    val scalaCompilerParamsOpt = artifacts.scalaOpt match {
      case Some(scalaArtifacts) =>
        val params = value(options.scalaParams).getOrElse {
          sys.error(
            "Should not happen (inconsistency between Scala parameters in BuildOptions and ScalaArtifacts)"
          )
        }

        val pluginScalacOptions = scalaArtifacts.compilerPlugins.distinct.map {
          case (_, _, path) =>
            ScalacOpt(s"-Xplugin:$path")
        }

        val semanticDbScalacOptions =
          if (generateSemanticDbs)
            if (params.scalaVersion.startsWith("2."))
              Seq(
                "-Yrangepos",
                "-P:semanticdb:failures:warning",
                "-P:semanticdb:synthetics:on",
                s"-P:semanticdb:sourceroot:${inputs.workspace}"
              ).map(ScalacOpt(_))
            else
              Seq(
                "-Xsemanticdb",
                "-sourceroot",
                inputs.workspace.toString
              ).map(ScalacOpt(_))
          else Nil

        val sourceRootScalacOptions =
          if (params.scalaVersion.startsWith("2.")) Nil
          else Seq("-sourceroot", inputs.workspace.toString).map(ScalacOpt(_))

        val scalaJsScalacOptions =
          if (options.platform.value == Platform.JS && !params.scalaVersion.startsWith("2."))
            Seq(ScalacOpt("-scalajs"))
          else Nil

        val scalacReleaseV =
          // the -release flag is not supported for Scala 2.12.x < 2.12.5
          if params.scalaVersion.asVersion < "2.12.5".asVersion then Nil
          else
            releaseFlagVersion
              .map(v => List("-release", v).map(ScalacOpt(_)))
              .getOrElse(Nil)

        val scalapyOptions =
          if (
            params.scalaVersion.startsWith("2.13.") &&
            options.notForBloopOptions.python.getOrElse(false)
          )
            Seq(ScalacOpt("-Yimports:java.lang,scala,scala.Predef,me.shadaj.scalapy"))
          else Nil

        val scalacOptions =
          options.scalaOptions.scalacOptions.map(_.value) ++
            pluginScalacOptions ++
            semanticDbScalacOptions ++
            sourceRootScalacOptions ++
            scalaJsScalacOptions ++
            scalacReleaseV ++
            scalapyOptions

        val compilerParams = ScalaCompilerParams(
          scalaVersion = params.scalaVersion,
          scalaBinaryVersion = params.scalaBinaryVersion,
          scalacOptions = scalacOptions.toSeq.map(_.value),
          compilerClassPath = scalaArtifacts.compilerClassPath
        )
        Some(compilerParams)

      case None =>
        None
    }

    val javacOptions = {

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

      val javacReleaseV = releaseFlagVersion.map(v => List("--release", v)).getOrElse(Nil)

      javacReleaseV ++ semanticDbJavacOptions ++ options.javaOptions.javacOptions.map(_.value)
    }

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
      scaladocDir = scaladocDir,
      scalaCompiler = scalaCompilerParamsOpt,
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
    compilerJvmVersionOpt: Option[Positioned[Int]],
    scope: Scope,
    compiler: ScalaCompiler,
    logger: Logger,
    buildClient: BloopBuildClient,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e)
  ): Either[BuildException, (os.Path, Option[ScalaParameters], Artifacts, Project, Boolean)] =
    either {

      val options0 =
        if (sources.hasJava && !sources.hasScala)
          options.copy(
            scalaOptions = options.scalaOptions.copy(
              scalaVersion = options.scalaOptions.scalaVersion.orElse {
                Some(MaybeScalaVersion.none)
              }
            )
          )
        else
          options
      val params = value(options0.scalaParams)

      val scopeParams =
        if (scope == Scope.Main) Nil
        else Seq(scope.name)

      buildClient.setProjectParams(scopeParams ++ value(options0.projectParams))

      val classesDir0 = classesDir(inputs.workspace, inputs.projectName, scope)

      val artifacts = value(options0.artifacts(logger, scope, maybeRecoverOnError))

      value(validate(logger, options0))

      val project = value {
        buildProject(
          inputs,
          sources,
          generatedSources,
          options0,
          compilerJvmVersionOpt,
          scope,
          logger,
          maybeRecoverOnError
        )
      }

      val projectChanged = compiler.prepareProject(project, logger)

      if (compiler.usesClassDir && projectChanged && os.isDir(classesDir0)) {
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

      (classesDir0, params, artifacts, project, projectChanged)
    }

  def buildOnce(
    inputs: Inputs,
    sources: Sources,
    generatedSources: Seq[GeneratedSource],
    options: BuildOptions,
    scope: Scope,
    logger: Logger,
    buildClient: BloopBuildClient,
    compiler: ScalaCompiler,
    partialOpt: Option[Boolean]
  ): Either[BuildException, Build] = either {

    if (options.platform.value == Platform.Native)
      value(scalaNativeSupported(options, inputs, logger)) match {
        case None        =>
        case Some(error) => value(Left(error))
      }

    val (classesDir0, scalaParams, artifacts, project, projectChanged) = value {
      prepareBuild(
        inputs,
        sources,
        generatedSources,
        options,
        compiler.jvmVersion,
        scope,
        compiler,
        logger,
        buildClient
      )
    }

    if (compiler.usesClassDir && projectChanged && os.isDir(classesDir0)) {
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

    val success = partial || compiler.compile(project, logger)

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
        @tailrec
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
    private var f: ScheduledFuture[?] = _
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
    compiler: ScalaCompiler,
    buildTests: Boolean,
    actionableDiagnostics: Option[Boolean]
  ): Either[BuildException, Option[Build]] = either {
    val jmhProjectName = inputs.projectName + "_jmh"
    val jmhOutputDir   = inputs.workspace / Constants.workspaceDirName / jmhProjectName
    os.remove.all(jmhOutputDir)
    val jmhSourceDir   = jmhOutputDir / "sources"
    val jmhResourceDir = jmhOutputDir / "resources"

    val retCode = run(
      javaCommand,
      build.fullClassPath.map(_.toIO),
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
          Directory(jmhSourceDir),
          ResourceDirectory(jmhResourceDir)
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
          compiler,
          None,
          crossBuilds = false,
          buildTests = buildTests,
          partial = None,
          actionableDiagnostics = actionableDiagnostics
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

    new ProcessBuilder(command*)
      .inheritIO()
      .start()
      .waitFor()
  }
}
