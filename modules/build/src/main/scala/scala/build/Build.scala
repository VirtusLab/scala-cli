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
import scala.build.compiler.{ScalaCompiler, ScalaCompilerMaker}
import scala.build.errors.*
import scala.build.input.*
import scala.build.internal.resource.ResourceMapper
import scala.build.internal.{Constants, MainClass, Name, Util}
import scala.build.options.*
import scala.build.options.validation.ValidationException
import scala.build.postprocessing.*
import scala.build.postprocessing.LineConversion.scalaLineToScLineShift
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
  def cancelled: Boolean
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
    isPartial: Boolean,
    logger: Logger
  ) extends Build {
    def success: Boolean                  = true
    def cancelled: Boolean                = false
    def successfulOpt: Some[this.type]    = Some(this)
    def outputOpt: Some[os.Path]          = Some(output)
    def dependencyClassPath: Seq[os.Path] = sources.resourceDirs ++ artifacts.classPath
    def fullClassPath: Seq[os.Path]       = Seq(output) ++ dependencyClassPath
    private lazy val mainClassesFoundInProject: Seq[String] = MainClass.find(output, logger).sorted
    private lazy val mainClassesFoundOnExtraClasspath: Seq[String] =
      options.classPathOptions.extraClassPath.flatMap(MainClass.find(_, logger)).sorted
    private lazy val mainClassesFoundInUserExtraDependencies: Seq[String] =
      artifacts.jarsForUserExtraDependencies.flatMap(MainClass.findInDependency).sorted
    def foundMainClasses(): Seq[String] = {
      val found = mainClassesFoundInProject ++ mainClassesFoundOnExtraClasspath
      if inputs.isEmpty && found.isEmpty then mainClassesFoundInUserExtraDependencies else found
    }
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
          case _              =>
            inferredMainClass(mainClasses, logger)
              .left.flatMap { mainClasses =>
                // decode the names to present them to the user,
                // but keep the link to each original name to account for package prefixes:
                // "pack.Main$minus1" decodes to "pack.Main-1", which encodes back to "pack$u002EMain$minus1"
                //  ^^^^^^^^^^^^^^^^----------------NOT THE SAME-----------------------^^^^^^^^^^^^^^^^^^^^^
                val decodedToEncoded = mainClasses.map(mc => Name.decoded(mc) -> mc).toMap

                options.interactive.flatMap { interactive =>
                  interactive
                    .chooseOne(
                      "Found several main classes. Which would you like to run?",
                      decodedToEncoded.keys.toList
                    )
                    .map(decodedToEncoded(_)) // encode back the name of the chosen class
                    .toRight {
                      SeveralMainClassesFoundError(
                        mainClasses = ::(mainClasses.head, mainClasses.tail.toList),
                        commandString = commandString,
                        positions = Nil
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
        sources.inMemory.collect {
          case Sources.InMemory(_, _, _, Some(wrapperParams)) =>
            wrapperParams.mainClass
        }
          .filter(mainClasses.contains(_))
      val rawInputInferredMainClasses =
        mainClasses
          .filterNot(scriptInferredMainClasses.contains(_))
          .filterNot(mainClassesFoundOnExtraClasspath.contains(_))
          .filterNot(mainClassesFoundInUserExtraDependencies.contains(_))
      val extraClasspathInferredMainClasses =
        mainClassesFoundOnExtraClasspath.filter(mainClasses.contains(_))
      val userExtraDependenciesInferredMainClasses =
        mainClassesFoundInUserExtraDependencies.filter(mainClasses.contains(_))

      def logMessageOnLesserPriorityMainClasses(
        pickedMainClass: String,
        mainClassDescriptor: String,
        lesserPriorityMainClasses: Seq[String]
      ): Unit =
        if lesserPriorityMainClasses.nonEmpty then {
          val first          = lesserPriorityMainClasses.head
          val completeString = lesserPriorityMainClasses.mkString(", ")
          logger.message(
            s"""Running $pickedMainClass. Also detected $mainClassDescriptor: $completeString
               |You can run any one of them by passing option --main-class, i.e. --main-class $first
               |All available main classes can always be listed by passing option --list-main-classes""".stripMargin
          )
        }

      (
        rawInputInferredMainClasses,
        scriptInferredMainClasses,
        extraClasspathInferredMainClasses,
        userExtraDependenciesInferredMainClasses
      ) match {
        case (Seq(pickedMainClass), scriptInferredMainClasses, _, _) =>
          logMessageOnLesserPriorityMainClasses(
            pickedMainClass = pickedMainClass,
            mainClassDescriptor = "script main classes",
            lesserPriorityMainClasses = scriptInferredMainClasses
          )
          Right(pickedMainClass)
        case (rawMcs, scriptMcs, extraCpMcs, userExtraDepsMcs) if rawMcs.length > 1 =>
          Left(rawMcs ++ scriptMcs ++ extraCpMcs ++ userExtraDepsMcs)
        case (Nil, Seq(pickedMainClass), _, _) => Right(pickedMainClass)
        case (Nil, scriptMcs, extraCpMcs, userExtraDepsMcs) if scriptMcs.length > 1 =>
          Left(scriptMcs ++ extraCpMcs ++ userExtraDepsMcs)
        case (Nil, Nil, Seq(pickedMainClass), userExtraDepsMcs) =>
          logMessageOnLesserPriorityMainClasses(
            pickedMainClass = pickedMainClass,
            mainClassDescriptor = "other main classes in dependencies",
            lesserPriorityMainClasses = userExtraDepsMcs
          )
          Right(pickedMainClass)
        case (Nil, Nil, extraCpMcs, userExtraDepsMcs) if extraCpMcs.length > 1 =>
          Left(extraCpMcs ++ userExtraDepsMcs)
        case (Nil, Nil, Nil, Seq(pickedMainClass)) => Right(pickedMainClass)
        case (Nil, Nil, Nil, userExtraDepsMcs) if userExtraDepsMcs.length > 1 =>
          Left(userExtraDepsMcs)
        case (rawMcs, scriptMcs, extraCpMcs, userExtraDepsMcs) =>
          Left(rawMcs ++ scriptMcs ++ extraCpMcs ++ userExtraDepsMcs)
      }
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
    def success: Boolean = false

    override def cancelled: Boolean = false
    def successfulOpt: None.type    = None
    def outputOpt: None.type        = None
  }

  final case class Cancelled(
    inputs: Inputs,
    options: BuildOptions,
    scope: Scope,
    reason: String
  ) extends Build {
    def success: Boolean         = false
    def cancelled: Boolean       = true
    def successfulOpt: None.type = None
    def outputOpt: None.type     = None
    def diagnostics: None.type   = None
  }

  /** If some options are manually overridden, append a hash of the options to the project name
    * Using only the command-line options not the ones from the sources.
    */
  def updateInputs(
    inputs: Inputs,
    options: BuildOptions
  ): Inputs = {

    // If some options are manually overridden, append a hash of the options to the project name
    // Using options, not options0 - only the command-line options are taken into account. No hash is
    // appended for options from the sources.
    val optionsHash = options.hash

    inputs.copy(baseProjectName = inputs.baseProjectName + optionsHash.fold("")("_" + _))
  }

  private def allInputs(
    inputs: Inputs,
    options: BuildOptions,
    logger: Logger
  )(using ScalaCliInvokeData) =
    CrossSources.forInputs(
      inputs,
      Sources.defaultPreprocessors(
        archiveCache = options.archiveCache,
        javaClassNameVersionOpt = options.internal.javaClassNameVersionOpt,
        javaCommand = () => options.javaHome().value.javaCommand
      ),
      logger,
      options.suppressWarningOptions,
      options.internal.exclude,
      download = options.downloader
    )

  private def build(
    inputs: Inputs,
    crossSources: CrossSources,
    options: BuildOptions,
    logger: Logger,
    buildClient: BloopBuildClient,
    compiler: ScalaCompiler,
    docCompilerOpt: Option[ScalaCompiler],
    crossBuilds: Boolean,
    buildTests: Boolean,
    partial: Option[Boolean],
    actionableDiagnostics: Option[Boolean]
  )(using ScalaCliInvokeData): Either[BuildException, Builds] = either {
    val sharedOptions = crossSources.sharedOptions(options)
    val crossOptions  = sharedOptions.crossOptions

    def doPostProcess(build: Build, inputs: Inputs, scope: Scope): Unit = build match {
      case build: Build.Successful =>
        for (sv <- build.project.scalaCompiler.map(_.scalaVersion))
          postProcess(
            generatedSources = build.generatedSources,
            generatedSrcRoot = inputs.generatedSrcRoot(scope),
            classesDir = build.output,
            logger = logger,
            workspace = inputs.workspace,
            updateSemanticDbs = true,
            scalaVersion = sv,
            buildOptions = build.options
          ).left.foreach(_.foreach(logger.message(_)))
      case _ =>
    }

    final case class NonCrossBuilds(
      main: Build,
      testOpt: Option[Build],
      docOpt: Option[Build],
      testDocOpt: Option[Build]
    )

    def doBuild(overrideOptions: BuildOptions): Either[BuildException, NonCrossBuilds] = either {

      val inputs0 = updateInputs(
        inputs,
        overrideOptions.orElse(options) // update hash in inputs with options coming from the CLI or cross-building, not from the sources
      )

      val baseOptions = overrideOptions.orElse(sharedOptions)

      val scopedSources: ScopedSources = value(crossSources.scopedSources(baseOptions))

      val mainSources: Sources =
        value(scopedSources.sources(Scope.Main, baseOptions, inputs.workspace, logger))
      val mainOptions = mainSources.buildOptions

      val testSources: Sources =
        value(scopedSources.sources(Scope.Test, baseOptions, inputs.workspace, logger))
      val testOptions = testSources.buildOptions

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
            inputs = inputs0,
            sources = sources0,
            generatedSources = generatedSources,
            options = options,
            scope = scope,
            logger = logger,
            buildClient = buildClient,
            compiler = actualCompiler,
            buildTests = buildTests,
            partial = partial,
            actionableDiagnostics = actionableDiagnostics
          )

          value(res)
        }

      val mainBuild       = value(doBuildScope(mainOptions, mainSources, Scope.Main))
      val mainDocBuildOpt = docCompilerOpt match {
        case None              => None
        case Some(docCompiler) =>
          Some(value(doBuildScope(
            options = mainOptions,
            sources = mainSources,
            scope = Scope.Main,
            actualCompiler = docCompiler
          )))
      }

      def testBuildOpt(doc: Boolean = false): Either[BuildException, Option[Build]] = either {
        if buildTests then {
          val actualCompilerOpt = if doc then docCompilerOpt else Some(compiler)
          actualCompilerOpt match {
            case None                 => None
            case Some(actualCompiler) =>
              val testBuild = value {
                mainBuild match {
                  case s: Build.Successful =>
                    val extraTestOptions = BuildOptions(
                      classPathOptions = ClassPathOptions(
                        extraClassPath = Seq(s.output)
                      )
                    )
                    val testOptions0 = {
                      val testOrExtra = extraTestOptions.orElse(testOptions)
                      testOrExtra
                        .copy(scalaOptions =
                          // Scala options between scopes need to be compatible
                          mainOptions.scalaOptions.orElse(testOrExtra.scalaOptions)
                        )
                    }
                    val isScala2 =
                      value(testOptions0.scalaParams).exists(_.scalaVersion.startsWith("2."))
                    val finalSources = if doc && isScala2 then
                      testSources.withExtraSources(mainSources)
                    else testSources
                    doBuildScope(
                      options = testOptions0,
                      sources = finalSources,
                      scope = Scope.Test,
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
          if crossBuilds then {
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
          else {
            if crossOptions.nonEmpty then {
              val crossBuildParams: Seq[CrossBuildParams] = crossOptions.map(CrossBuildParams(_))
              logger.message(
                s"""Cross-building is disabled, ignoring ${crossOptions.length} builds:
                   |  ${crossBuildParams.map(_.asString).mkString("\n  ")}
                   |Cross builds are only available when the --cross option is passed.
                   |Defaulting to ${CrossBuildParams(options).asString}""".stripMargin
              )
            }
            (Nil, Nil, Nil, Nil)
          }

        Builds(
          builds = Seq(nonCrossBuilds.main) ++ nonCrossBuilds.testOpt.toSeq,
          crossBuilds = Seq(extraMainBuilds, extraTestBuilds),
          docBuilds = nonCrossBuilds.docOpt.toSeq ++ nonCrossBuilds.testDocOpt.toSeq,
          docCrossBuilds = Seq(extraDocBuilds, extraDocTestBuilds)
        )
      }

    val builds = value(buildScopes())

    ResourceMapper.copyResourceToClassesDir(builds.main)
    for (testBuild <- builds.get(Scope.Test))
      ResourceMapper.copyResourceToClassesDir(testBuild)

    if actionableDiagnostics.getOrElse(true) then {
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
  )(using ScalaCliInvokeData): Either[BuildException, Build] = either {

    val build0 = value {
      buildOnce(
        inputs = inputs,
        sources = sources,
        generatedSources = generatedSources,
        options = options,
        scope = scope,
        logger = logger,
        buildClient = buildClient,
        compiler = compiler,
        partialOpt = partial
      )
    }

    build0 match {
      case successful: Successful =>
        if options.jmhOptions.canRunJmh && scope == Scope.Main then
          value {
            val res = jmhBuild(
              inputs = inputs,
              build = successful,
              logger = logger,
              successful.options.javaHome().value.javaCommand,
              buildClient = buildClient,
              compiler = compiler,
              buildTests = buildTests,
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

  def projectRootDir(root: os.Path, projectName: String): os.Path =
    root / Constants.workspaceDirName / projectName
  def classesRootDir(root: os.Path, projectName: String): os.Path =
    projectRootDir(root, projectName) / "classes"
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
        def snCompatError      =
          Left(
            new ScalaNativeCompatibilityError(
              scalaVersion,
              options.scalaNativeOptions.finalVersion
            )
          )
        def warnIncompatibleNativeOptions(numeralVersion: SNNumeralVersion) =
          if numeralVersion < SNNumeralVersion(0, 4, 4)
            && options.scalaNativeOptions.embedResources.isDefined
          then
            logger.diagnostic(
              "This Scala Version cannot embed resources, regardless of the options used."
            )

        val numeralOrError: Either[ScalaNativeCompatibilityError, SNNumeralVersion] =
          nativeVersionMaybe match {
            case Some(snNumeralVer) =>
              if snNumeralVer < SNNumeralVersion(0, 4, 1) && Properties.isWin then snCompatError
              else if scalaVersion.startsWith("3.0") then snCompatError
              else if scalaVersion.startsWith("3") then
                if snNumeralVer >= SNNumeralVersion(0, 4, 3)
                then Right(snNumeralVer)
                else snCompatError
              else if scalaVersion.startsWith("2.13") then Right(snNumeralVer)
              else if scalaVersion.startsWith("2.12") then
                if inputs.sourceFiles().forall {
                    case _: AnyScript => snNumeralVer >= SNNumeralVersion(0, 4, 3)
                    case _            => true
                  }
                then Right(snNumeralVer)
                else snCompatError
              else snCompatError
            case None => snCompatError
          }

        numeralOrError match {
          case Left(compatError)       => Some(compatError)
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
  )(using ScalaCliInvokeData): Either[BuildException, Builds] = either {
    val buildClient = BloopBuildClient.create(
      logger = logger,
      keepDiagnostics = options.internal.keepDiagnostics
    )
    val classesDir0                           = classesRootDir(inputs.workspace, inputs.projectName)
    val (crossSources: CrossSources, inputs0) = value(allInputs(inputs, options, logger))
    val buildOptions                          = crossSources.sharedOptions(options)
    if !buildOptions.suppressWarningOptions.suppressDeprecatedFeatureWarning.getOrElse(false) &&
      buildOptions.scalaParams.exists(_.exists(_.scalaVersion == "2.12.4") &&
      !buildOptions.useBuildServer.contains(false))
    then
      logger.message(
        s"""[${Console.YELLOW}warn${Console.RESET}] Scala 2.12.4 has been deprecated for use with Bloop.
           |[${Console.YELLOW}warn${Console.RESET}] It may lead to infinite compilation.
           |[${Console.YELLOW}warn${Console.RESET}] To disable the build server, pass ${Console.BOLD}--server=false${Console.RESET}.
           |[${Console.YELLOW}warn${Console.RESET}] Refer to https://github.com/VirtusLab/scala-cli/issues/1382 and https://github.com/sbt/zinc/issues/1010""".stripMargin
      )
    value {
      compilerMaker.withCompiler(
        workspace = inputs0.workspace / Constants.workspaceDirName,
        classesDir = classesDir0,
        buildClient = buildClient,
        logger = logger,
        buildOptions = buildOptions
      ) { compiler =>
        docCompilerMakerOpt match {
          case None =>
            logger.debug("No doc compiler provided, skipping")
            build(
              inputs = inputs0,
              crossSources = crossSources,
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
              workspace = inputs0.workspace / Constants.workspaceDirName,
              classesDir = classesDir0, // ???
              buildClient = buildClient,
              logger = logger,
              buildOptions = buildOptions
            ) { docCompiler =>
              build(
                inputs = inputs0,
                crossSources = crossSources,
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
  }

  def validate(
    logger: Logger,
    options: BuildOptions
  ): Either[BuildException, Unit] = {
    val (errors, otherDiagnostics) = options.validate.partition(_.severity == Severity.Error)
    logger.log(otherDiagnostics)
    if errors.nonEmpty then Left(CompositeBuildException(errors.map(new ValidationException(_))))
    else Right(())
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
  )(action: Either[BuildException, Builds] => Unit)(using ScalaCliInvokeData): Watcher = {

    val buildClient = BloopBuildClient.create(
      logger,
      keepDiagnostics = options.internal.keepDiagnostics
    )
    val threads     = BuildThreads.create()
    val classesDir0 = classesRootDir(inputs.workspace, inputs.projectName)

    lazy val compilers: Either[BuildException, (ScalaCompiler, Option[ScalaCompiler])] =
      either {
        val (crossSources: CrossSources, inputs0: Inputs) =
          value(allInputs(inputs, options, logger))
        val sharedOptions = crossSources.sharedOptions(options)
        val compiler      = value {
          compilerMaker.create(
            workspace = inputs0.workspace / Constants.workspaceDirName,
            classesDir = classesDir0,
            buildClient = buildClient,
            logger = logger,
            buildOptions = sharedOptions
          )
        }
        val docCompilerOpt = docCompilerMakerOpt.map(_.create(
          workspace = inputs0.workspace / Constants.workspaceDirName,
          classesDir = classesDir0,
          buildClient = buildClient,
          logger = logger,
          buildOptions = sharedOptions
        )).map(value)
        compiler -> docCompilerOpt
      }

    def info: Either[BuildException, (ScalaCompiler, Option[ScalaCompiler], CrossSources, Inputs)] =
      either {
        val (crossSources: CrossSources, inputs0: Inputs) =
          value(allInputs(inputs, options, logger))
        val (compiler, docCompilerOpt) = value(compilers)
        (compiler, docCompilerOpt, crossSources, inputs0)
      }

    var res: Either[BuildException, Builds] = null

    def run(): Unit = {
      try {
        res =
          info.flatMap {
            case (
                  compiler: ScalaCompiler,
                  docCompilerOpt: Option[ScalaCompiler],
                  crossSources: CrossSources,
                  inputs: Inputs
                ) =>
              build(
                inputs = inputs,
                crossSources = crossSources,
                options = options,
                logger = logger,
                buildClient = buildClient,
                compiler = compiler,
                docCompilerOpt = docCompilerOpt,
                crossBuilds = crossBuilds,
                buildTests = buildTests,
                partial = partial,
                actionableDiagnostics = actionableDiagnostics
              )
          }
        action(res)
      }
      catch {
        case NonFatal(e) =>
          Util.printException(e)
      }
      postAction()
    }

    run()

    val watcher =
      new Watcher(ListBuffer(), threads.fileWatcher, run(), info.foreach(_._1.shutdown()))

    def doWatch(): Unit = {
      val elements: Seq[Element] =
        if res == null then inputs.elements
        else
          res
            .map { builds =>
              val mainElems = builds.main.inputs.elements
              val testElems = builds.get(Scope.Test).map(_.inputs.elements).getOrElse(Nil)
              (mainElems ++ testElems).distinct
            }
            .getOrElse(inputs.elements)
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
              val pathLast    = relPath.lastOpt.orElse(p.lastOpt).getOrElse("")
              def isScalaFile = pathLast.endsWith(".sc") || pathLast.endsWith(".scala")
              def isJavaFile  = pathLast.endsWith(".java")
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
          onChangeBufferedObserver(event => if eventFilter(event) then watcher.schedule())
        }
      }

      val artifacts = res
        .map { builds =>
          def artifacts(build: Build): Seq[os.Path] =
            build.successfulOpt.toSeq.flatMap(_.artifacts.classPath)
          val main               = artifacts(builds.main)
          val test               = builds.get(Scope.Test).map(artifacts).getOrElse(Nil)
          val allScopesArtifacts = (main ++ test).distinct

          allScopesArtifacts
            .filterNot(_.segments.contains(Constants.workspaceDirName))
        }
        .getOrElse(Nil)
      for (artifact <- artifacts) {
        val depth    = if os.isFile(artifact) then -1 else Int.MaxValue
        val watcher0 = watcher.newWatcher()
        watcher0.register(artifact.toNIO, depth)
        watcher0.addObserver(onChangeBufferedObserver(_ => watcher.schedule()))
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
    if compilerJvmVersionOpt.exists(javaHome.value.version > _.value) then {
      logger.log(List(Diagnostic(
        Diagnostic.Messages.bloopTooOld,
        Severity.Warning,
        javaHome.positions ++ compilerJvmVersionOpt.map(_.positions).getOrElse(Nil)
      )))
      None
    }
    else if compilerJvmVersionOpt.exists(_.value == 8) then None
    else if options.scalaOptions.scalacOptions.values.exists(opt =>
        opt.headOption.exists(_.value.value.startsWith("-release")) ||
        opt.headOption.exists(_.value.value.startsWith("-java-output-version"))
      )
    then None
    else if compilerJvmVersionOpt.isEmpty && javaHome.value.version == 8 then None
    else Some(javaHome.value.version)
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
    artifacts: Artifacts,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e)
  ): Either[BuildException, Project] = either {

    val allSources = sources.paths.map(_._1) ++ generatedSources.map(_.generated)

    val classesDir0 = classesDir(inputs.workspace, inputs.projectName, scope)
    val scaladocDir = classesDir(inputs.workspace, inputs.projectName, scope, suffix = "-doc")

    val generateSemanticDbs =
      options.scalaOptions.semanticDbOptions.generateSemanticDbs.getOrElse(false)
    val semanticDbTargetRoot = options.scalaOptions.semanticDbOptions.semanticDbTargetRoot
    val semanticDbSourceRoot =
      options.scalaOptions.semanticDbOptions.semanticDbSourceRoot.getOrElse(inputs.workspace)

    val scalaCompilerParamsOpt = artifacts.scalaOpt match {
      case Some(scalaArtifacts) =>
        val params = value(options.scalaParams).getOrElse {
          sys.error(
            "Should not happen (inconsistency between Scala parameters in BuildOptions and ScalaArtifacts)"
          )
        }

        val pluginScalacOptions = scalaArtifacts.compilerPlugins.map {
          case (_, _, path) =>
            ScalacOpt(s"-Xplugin:$path")
        }.distinct

        val semanticDbTargetRootOptions: Seq[ScalacOpt] =
          (semanticDbTargetRoot match
            case Some(targetRoot) if params.scalaVersion.startsWith("2.") =>
              Seq(s"-P:semanticdb:targetroot:$targetRoot")
            case Some(targetRoot) => Seq("-semanticdb-target", targetRoot.toString)
            case None             => Nil
          ).map(ScalacOpt(_))
        val semanticDbScalacOptions: Seq[ScalacOpt] =
          if generateSemanticDbs then
            semanticDbTargetRootOptions ++ (
              if params.scalaVersion.startsWith("2.") then
                Seq(
                  "-Yrangepos",
                  "-P:semanticdb:failures:warning",
                  "-P:semanticdb:synthetics:on",
                  s"-P:semanticdb:sourceroot:$semanticDbSourceRoot"
                )
              else Seq("-Xsemanticdb", "-sourceroot", semanticDbSourceRoot.toString)
            ).map(ScalacOpt(_))
          else Nil

        val sourceRootScalacOptions =
          if params.scalaVersion.startsWith("2.")
          then Nil
          else Seq("-sourceroot", inputs.workspace.toString).map(ScalacOpt(_))

        val scalaJsScalacOptions =
          if options.platform.value == Platform.JS && !params.scalaVersion.startsWith("2.")
          then Seq(ScalacOpt("-scalajs"))
          else Nil

        val scalapyOptions =
          if params.scalaVersion.startsWith("2.13.") &&
            options.notForBloopOptions.python.getOrElse(false)
          then Seq(ScalacOpt("-Yimports:java.lang,scala,scala.Predef,me.shadaj.scalapy"))
          else Nil

        val scalacOptions =
          options.scalaOptions.scalacOptions.map(_.value) ++
            pluginScalacOptions ++
            semanticDbScalacOptions ++
            sourceRootScalacOptions ++
            scalaJsScalacOptions ++
            scalapyOptions

        val compilerParams = ScalaCompilerParams(
          scalaVersion = params.scalaVersion,
          scalaBinaryVersion = params.scalaBinaryVersion,
          scalacOptions = scalacOptions.toSeq.map(_.value),
          compilerClassPath = scalaArtifacts.compilerClassPath,
          bridgeJarsOpt = scalaArtifacts.bridgeJarsOpt.map(_.headOption.toSeq)
        )
        Some(compilerParams)

      case None =>
        None
    }

    val javacOptions = {

      val semanticDbJavacOptions =
        // FIXME Should this be in scalaOptions, now that we use it for javac stuff too?
        if generateSemanticDbs then {
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

          val javacTargetRoot = semanticDbTargetRoot.getOrElse("javac-classes-directory")
          Seq(
            // does the path need to be escaped somehow?
            s"-Xplugin:semanticdb -sourceroot:$semanticDbSourceRoot -targetroot:$javacTargetRoot"
          ) ++ exports
        }
        else
          Nil

      semanticDbJavacOptions ++ options.javaOptions.javacOptions.map(_.value)
    }

    // `test` scope should contains class path to main scope
    val mainClassesPath =
      if scope == Scope.Test
      then List(classesDir(inputs.workspace, inputs.projectName, Scope.Main))
      else Nil

    value(validate(logger, options))

    val fullClassPath = artifacts.compileClassPath ++
      mainClassesPath ++
      artifacts.javacPluginDependencies.map(_._3) ++
      artifacts.extraJavacPlugins

    val project = Project(
      directory = inputs.workspace / Constants.workspaceDirName,
      argsFilePath =
        projectRootDir(inputs.workspace, inputs.projectName) / Constants.scalacArgumentsFileName,
      workspace = inputs.workspace,
      classesDir = classesDir0,
      scaladocDir = scaladocDir,
      scalaCompiler = scalaCompilerParamsOpt,
      scalaJsOptions =
        if options.platform.value == Platform.JS
        then Some(value(options.scalaJsOptions.config(logger)))
        else None,
      scalaNativeOptions =
        if options.platform.value == Platform.Native
        then Some(options.scalaNativeOptions.bloopConfig())
        else None,
      projectName = inputs.scopeProjectName(scope),
      classPath = fullClassPath,
      resolution = Some(Project.resolution(artifacts.detailedArtifacts)),
      sources = allSources,
      resourceDirs = sources.resourceDirs,
      scope = scope,
      javaHomeOpt = Option(options.javaHomeLocation().value),
      javacOptions = javacOptions.toList
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
        // FIXME: don't add Scala to pure Java test builds (need to add pure Java test runner)
        if sources.hasJava && !sources.hasScala && scope != Scope.Test
        then
          options.copy(
            scalaOptions = options.scalaOptions.copy(
              scalaVersion = options.scalaOptions.scalaVersion.orElse {
                Some(MaybeScalaVersion.none)
              }
            )
          )
        else options
      val params = value(options0.scalaParams)

      val scopeParams = if scope == Scope.Main then Nil else Seq(scope.name)

      buildClient.setProjectParams(scopeParams ++ value(options0.projectParams))

      val classesDir0 = classesDir(inputs.workspace, inputs.projectName, scope)

      val artifacts = value(options0.artifacts(logger, scope, maybeRecoverOnError))

      value(validate(logger, options0))

      val project = value {
        buildProject(
          inputs = inputs,
          sources = sources,
          generatedSources = generatedSources,
          options = options0,
          compilerJvmVersionOpt = compilerJvmVersionOpt,
          scope = scope,
          logger = logger,
          artifacts = artifacts,
          maybeRecoverOnError = maybeRecoverOnError
        )
      }

      val projectChanged = compiler.prepareProject(project, logger)

      if projectChanged then {
        if compiler.usesClassDir && os.isDir(classesDir0) then {
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
        if os.exists(project.argsFilePath) then {
          logger.debug(s"Removing ${project.argsFilePath}")
          try os.remove(project.argsFilePath)
          catch {
            case ex: FileSystemException =>
              logger.debug(s"Ignoring $ex while cleaning up ${project.argsFilePath}")
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

    if options.platform.value == Platform.Native then
      value(scalaNativeSupported(options, inputs, logger)) match {
        case None        =>
        case Some(error) => value(Left(error))
      }

    val (classesDir0, scalaParams, artifacts, project, projectChanged) = value {
      prepareBuild(
        inputs = inputs,
        sources = sources,
        generatedSources = generatedSources,
        options = options,
        compilerJvmVersionOpt = compiler.jvmVersion,
        scope = scope,
        compiler = compiler,
        logger = logger,
        buildClient = buildClient
      )
    }

    buildClient.clear()
    buildClient.setGeneratedSources(scope, generatedSources)

    val partial = partialOpt.getOrElse {
      options.notForBloopOptions.packageOptions.packageTypeOpt.exists(_.sourceBased)
    }

    val success = partial || compiler.compile(project, logger)

    if success then
      Successful(
        inputs = inputs,
        options = options,
        scalaParams,
        scope = scope,
        sources = sources,
        artifacts = artifacts,
        project = project,
        output = classesDir0,
        diagnostics = buildClient.diagnostics,
        generatedSources = generatedSources,
        isPartial = partial,
        logger = logger
      )
    else
      Failed(
        inputs = inputs,
        options = options,
        scope = scope,
        sources = sources,
        artifacts = artifacts,
        project = project,
        diagnostics = buildClient.diagnostics
      )
  }

  def postProcess(
    generatedSources: Seq[GeneratedSource],
    generatedSrcRoot: os.Path,
    classesDir: os.Path,
    logger: Logger,
    workspace: os.Path,
    updateSemanticDbs: Boolean,
    scalaVersion: String,
    buildOptions: BuildOptions
  ): Either[Seq[String], Unit] =
    if os.exists(classesDir) then {

      // TODO Write classes to a separate directory during post-processing
      logger.debug("Post-processing class files of pre-processed sources")
      val mappings = generatedSources
        .map { source =>
          val relPath       = source.generated.relativeTo(generatedSrcRoot).toString
          val reportingPath = source.reportingPath.fold(s => s, _.last)
          (relPath, (reportingPath, scalaLineToScLineShift(source.wrapperParamsOpt)))
        }
        .toMap

      val postProcessors =
        Seq(ByteCodePostProcessor) ++
          (if updateSemanticDbs then Seq(SemanticDbPostProcessor) else Nil) ++
          Seq(TastyPostProcessor)

      val failures = postProcessors.flatMap(
        _.postProcess(
          generatedSources = generatedSources,
          mappings = mappings,
          workspace = workspace,
          output = classesDir,
          logger = logger,
          scalaVersion = scalaVersion,
          buildOptions = buildOptions
        )
          .fold(e => Seq(e), _ => Nil)
      )
      if failures.isEmpty then Right(()) else Left(failures)
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
          if t != null then {
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
    private val runnable: Runnable    = { () =>
      lock.synchronized {
        f = null
      }
      onChange // FIXME Log exceptions
    }
    def schedule(): Unit =
      if f == null then
        lock.synchronized {
          if f == null then f = scheduler.schedule(runnable, waitFor.length, waitFor.unit)
        }
  }

  private def printable(path: os.Path): String =
    if path.startsWith(os.pwd) then path.relativeTo(os.pwd).toString
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
  )(using ScalaCliInvokeData): Either[BuildException, Option[Build]] = either {
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
    if retCode != 0 then {
      val red      = Console.RED
      val lightRed = "\u001b[91m"
      val reset    = Console.RESET
      System.err.println(
        s"${red}jmh bytecode generator exited with return code $lightRed$retCode$red.$reset"
      )
    }

    if retCode == 0 then {
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
      val (crossSources, inputs0) = value(allInputs(jmhInputs, updatedOptions, logger))
      val jmhBuilds               = value {
        Build.build(
          inputs0,
          crossSources,
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
