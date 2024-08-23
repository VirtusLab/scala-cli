package scala.build.compose

import dependency.ScalaParameters

import java.nio.file.FileSystemException
import scala.build.*
import scala.build.EitherCps.{either, value}
import scala.build.bsp.BspServer
import scala.build.bsp.buildtargets.ProjectName
import scala.build.compiler.ScalaCompiler
import scala.build.errors.{BuildException, Diagnostic}
import scala.build.input.{Module, ScalaCliInvokeData}
import scala.build.options.{BuildOptions, MaybeScalaVersion, Scope}

object ComposeBuild {

  final case class PreBuildData(
    sources: Sources,
    buildOptions: BuildOptions,
    classesDir: os.Path,
    scalaParams: Option[ScalaParameters],
    artifacts: Artifacts,
    project: Project,
    generatedSources: Seq[GeneratedSource],
    buildChanged: Boolean = false
  )

  final case class PreBuildProject(prebuildModules: Seq[PreBuildModule])

  final case class PreBuildModule(
    module: Module,
    mainScope: PreBuildData,
    testScope: PreBuildData,
    diagnostics: Seq[Diagnostic]
  )
}

case class ComposeBuild(
  buildOptions: BuildOptions,
  inputs: Inputs,
  logger: Logger,
  compiler: ScalaCompiler,
  buildClient: BloopBuildClient,
  bspServer: Option[BspServer],
  actionableDiagnostics: Option[Boolean],
  verbosity: Int = 0,
  maybeRecoverOnError: ProjectName => BuildException => Option[BuildException] = _ => e => Some(e)
)(using ScalaCliInvokeData) {

  import ComposeBuild.*

  /** Prepares the build for all modules in the inputs, */
  def prepareBuild(): Either[(BuildException, ProjectName), PreBuildProject] = inputs match
    case composeInputs: ComposedInputs=> prepareComposeBuild(composeInputs)
    case SimpleInputs(singleModule) =>
      for (singlePreBuildModule <- prepareModule(singleModule))
        yield PreBuildProject(Seq(singlePreBuildModule))

  private def prepareComposeBuild(composeInputs: ComposedInputs): Either[(BuildException, ProjectName), PreBuildProject] = either {
    logger.log("Preparing composed build")

    val prebuildModules: Seq[PreBuildModule] =
      for (module <- inputs.modulesBuildOrder) yield value(prepareModule(module))

    val prebuildModulesWithLinkedDeps = {
      val preBuildDataMap: Map[ProjectName, PreBuildModule] =
        prebuildModules.map(m => m.module.projectName -> m).toMap

      prebuildModules.map { prebuildModule =>
        val additionalMainClassPath = prebuildModule.module.moduleDependencies
          .map(preBuildDataMap)
          .flatMap(_.mainScope.project.classPath)
        val oldMainProject = prebuildModule.mainScope.project
        val newMainProject = oldMainProject.copy(
          classPath = (oldMainProject.classPath.toSet ++ additionalMainClassPath).toSeq
        )

        val mainProjectChanged = writeProject(newMainProject)

        pprint.err.log(oldMainProject.classPath)
        pprint.err.log(newMainProject.classPath)

        val additionalTestClassPath = prebuildModule.module.moduleDependencies
          .map(preBuildDataMap)
          .flatMap(_.testScope.project.classPath)
        val oldTestProject = prebuildModule.testScope.project
        val newTestProject = oldTestProject.copy(
          classPath =
            (oldTestProject.classPath.toSet ++ additionalMainClassPath ++ additionalTestClassPath).toSeq
        )

        val testProjectChanged = writeProject(newTestProject)

        prebuildModule.copy(
          mainScope = prebuildModule.mainScope.copy(project = newMainProject, buildChanged = mainProjectChanged),
          testScope = prebuildModule.testScope.copy(project = newTestProject, buildChanged = testProjectChanged)
        )
      }
    }

    PreBuildProject(prebuildModulesWithLinkedDeps)
  }

  private def prepareModule(module: Module): Either[(BuildException, ProjectName), PreBuildModule] =
    either {
      val persistentLogger = new PersistentDiagnosticLogger(logger)
      val mainProjectName  = module.projectName
      val testProjectName  = module.scopeProjectName(Scope.Test)

      // allInputs contains elements from using directives
      val (crossSources, allInputs) = value {
        CrossSources.forModuleInputs(
          inputs = module,
          preprocessors = Sources.defaultPreprocessors(
            buildOptions.archiveCache,
            buildOptions.internal.javaClassNameVersionOpt,
            () => buildOptions.javaHome().value.javaCommand
          ),
          logger = persistentLogger,
          suppressWarningOptions = buildOptions.suppressWarningOptions,
          exclude = buildOptions.internal.exclude,
          maybeRecoverOnError = maybeRecoverOnError(mainProjectName)
        ).left.map(_ -> mainProjectName)
      }

      val sharedOptions = crossSources.sharedOptions(buildOptions)

      if (verbosity >= 4)
        pprint.err.log(crossSources)

      val scopedSources =
        value(crossSources.scopedSources(buildOptions).left.map(_ -> mainProjectName))

      if (verbosity >= 4)
        pprint.err.log(scopedSources)

      val sourcesMain = value {
        scopedSources.sources(Scope.Main, sharedOptions, allInputs.workspace, persistentLogger)
          .left.map(_ -> mainProjectName)
      }

      val sourcesTest = value {
        scopedSources.sources(Scope.Test, sharedOptions, allInputs.workspace, persistentLogger)
          .left.map(_ -> testProjectName)
      }

      if (verbosity >= 4)
        pprint.err.log(sourcesMain)

      val options0Main = sourcesMain.buildOptions
      val options0Test = sourcesTest.buildOptions.orElse(options0Main)

      val generatedSourcesMain =
        sourcesMain.generateSources(allInputs.generatedSrcRoot(Scope.Main))
      val generatedSourcesTest =
        sourcesTest.generateSources(allInputs.generatedSrcRoot(Scope.Test))

      // Notify the Bsp server (if there is any) about changes to the project params
      bspServer.foreach(_.setExtraDependencySources(options0Main.classPathOptions.extraSourceJars))
      bspServer.foreach(
        _.setExtraTestDependencySources(options0Test.classPathOptions.extraSourceJars)
      )
      bspServer.foreach(_.setGeneratedSources(mainProjectName, generatedSourcesMain))
      bspServer.foreach(_.setGeneratedSources(testProjectName, generatedSourcesTest))

      // Notify the build client about generated sources so that it can modify diagnostics coming to the remote client e.g. IDE or console (not really a client, but you get it)
      buildClient.setGeneratedSources(mainProjectName, generatedSourcesMain)
      buildClient.setGeneratedSources(testProjectName, generatedSourcesTest)

      val (classesDir0Main, scalaParamsMain, artifactsMain, projectMain) = value {
        val res = prepareProject(
          allInputs,
          sourcesMain,
          generatedSourcesMain,
          options0Main,
          None,
          Scope.Main,
          compiler,
          persistentLogger,
          buildClient,
          maybeRecoverOnError(mainProjectName)
        )
        res.left.map(_ -> mainProjectName)
      }

      val (classesDir0Test, scalaParamsTest, artifactsTest, projectTest) = value {
        val res = prepareProject(
          allInputs,
          sourcesTest,
          generatedSourcesTest,
          options0Test,
          None,
          Scope.Test,
          compiler,
          persistentLogger,
          buildClient,
          maybeRecoverOnError(testProjectName)
        )
        res.left.map(_ -> testProjectName)
      }

      val mainScope = PreBuildData(
        sourcesMain,
        options0Main,
        classesDir0Main,
        scalaParamsMain,
        artifactsMain,
        projectMain,
        generatedSourcesMain
      )

      val testScope = PreBuildData(
        sourcesTest,
        options0Test,
        classesDir0Test,
        scalaParamsTest,
        artifactsTest,
        projectTest,
        generatedSourcesTest
      )

      if (actionableDiagnostics.getOrElse(true)) {
        val projectOptions = options0Test.orElse(options0Main)
        projectOptions.logActionableDiagnostics(persistentLogger)
      }

      PreBuildModule(module, mainScope, testScope, persistentLogger.diagnostics)
    }

  //FIXME It's a copied part of Build.prepareBuild()
  private def prepareProject(
                            inputs: Module,
                            sources: Sources,
                            generatedSources: Seq[GeneratedSource],
                            options: BuildOptions,
                            compilerJvmVersionOpt: Option[Positioned[Int]],
                            scope: Scope,
                            compiler: ScalaCompiler,
                            logger: Logger,
                            buildClient: BloopBuildClient,
                            maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e)
                          ): Either[BuildException, (os.Path, Option[ScalaParameters], Artifacts, Project)] = either {

    val options0 =
      // FIXME: don't add Scala to pure Java test builds (need to add pure Java test runner)
      if (sources.hasJava && !sources.hasScala && scope != Scope.Test)
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

    val classesDir0 = Build.classesDir(inputs.workspace, inputs.projectName, scope)

    val artifacts = value(options0.artifacts(logger, scope, maybeRecoverOnError))

    value(Build.validate(logger, options0))

    val project = value {
      Build.buildProject(
        inputs,
        sources,
        generatedSources,
        options0,
        compilerJvmVersionOpt,
        scope,
        logger,
        artifacts,
        maybeRecoverOnError
      )
    }

    (classesDir0, params, artifacts, project)
  }

  //FIXME It's a copied part of Build.prepareBuild()
  private def writeProject(project: Project): Boolean = {
    val projectChanged = compiler.prepareProject(project, logger)

    if (projectChanged) {
      if (compiler.usesClassDir && os.isDir(project.classesDir)) {
        logger.debug(s"Clearing ${project.classesDir}")
        os.list(project.classesDir).foreach { p =>
          logger.debug(s"Removing $p")
          try os.remove.all(p)
          catch {
            case ex: FileSystemException =>
              logger.debug(s"Ignoring $ex while cleaning up $p")
          }
        }
      }
      if (os.exists(project.argsFilePath)) {
        logger.debug(s"Removing ${project.argsFilePath}")
        try os.remove(project.argsFilePath)
        catch {
          case ex: FileSystemException =>
            logger.debug(s"Ignoring $ex while cleaning up ${project.argsFilePath}")
        }
      }
    }
    projectChanged
  }

}
