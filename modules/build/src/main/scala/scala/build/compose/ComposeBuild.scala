package scala.build.compose

import scala.build.{BloopBuildClient, CrossSources, Logger}
import scala.build.bsp.BspServer
import scala.build.bsp.buildtargets.ProjectName
import scala.build.compose.input as compose
import scala.build.errors.BuildException
import scala.build.options.BuildOptions
import scala.build.compiler.ScalaCompiler
import scala.build.{Artifacts, Project}
import scala.build.input.Module

object ComposeBuild {

  private final case class PreBuildData(
                                         sources: Sources,
                                         buildOptions: BuildOptions,
                                         classesDir: os.Path,
                                         scalaParams: Option[ScalaParameters],
                                         artifacts: Artifacts,
                                         project: Project,
                                         generatedSources: Seq[GeneratedSource],
                                         buildChanged: Boolean
                                       )

  private final case class PreBuildProject(prebuildModules: Seq[PreBuildModule])

  private final case class PreBuildModule(
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
                         verbosity: Int = 0,
                         maybeRecoverOnError: ProjectName => BuildException => Option[BuildException] = _ => e => Some(e)
                       ) {

  import ComposeBuild.*

  /** Prepares the build for all modules in the inputs, */
  def prepareBuild(): Either[(BuildException, ProjectName), PreBuildProject] = either {
    logger.log("Preparing composed build")

    val prebuildModules: Seq[PreBuildModule] = for (module <- inputs.modulesBuildOrder) yield {
      value(prepareModule(module))
    }

    val prebuildModulesWithLinkedDeps = {
      val preBuildDataMap: Map[ProjectName, PreBuildModule] = prebuildModules.map(m => m.module.projectName -> m)

      prebuildModules.map { prebuildModule =>
        val additionalMainClassPath = prebuildModule.module.moduleDependencies
          .map(preBuildDataMap)
          .flatMap(_.mainScope.project.classPath)
        val oldMainProject = prebuildModule.mainScope.project
        val newMainProject = oldMainProject.copy(
          classPath = (oldMainProject.classPath.toSet ++ additionalMainClassPath).toSeq
        )

        val additionalTestClassPath = prebuildModule.module.moduleDependencies
          .map(preBuildDataMap)
          .flatMap(_.testScope.project.classPath)
        val oldTestProject = prebuildModule.testScope.project
        val newTestProject = oldTestProject.copy(
          classPath = (oldTestProject.classPath.toSet ++ additionalMainClassPath ++ additionalTestClassPath).toSeq
        )

        prebuildModule.copy(
          mainScope = prebuildModule.mainScope.copy(project = newMainProject),
          testScope = prebuildModule.mainScope.copy(project = newTestProject)
        )
      }
    }

    PreBuildProject(prebuildModulesWithLinkedDeps)
  }

  def prepareModule(module: Module): Either[(BuildException, ProjectName), PreBuildModule] = either {
    val mainProjectName = module.projectName
    val testProjectName = module.scopeProjectName(Scope.Test)

    // allInputs contains elements from using directives
    val (crossSources, allInputs) = value {
      CrossSources.forModuleInputs(
        inputs = module,
        preprocessors = Sources.defaultPreprocessors(
          buildOptions.archiveCache,
          buildOptions.internal.javaClassNameVersionOpt,
          () => buildOptions.javaHome().value.javaCommand
        ),
        logger = logger,
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
    bspServer.foreach(_.setExtraTestDependencySources(options0Test.classPathOptions.extraSourceJars))
    bspServer.foreach(_.setGeneratedSources(mainProjectName, generatedSourcesMain))
    bspServer.foreach(_.setGeneratedSources(testProjectName, generatedSourcesTest))

    // Notify the build client about generated sources so that it can modify diagnostics coming to the remote client e.g. IDE or console (not really a client, but you get it)
    buildClient.setGeneratedSources(mainProjectName, generatedSourcesMain)
    buildClient.setGeneratedSources(testProjectName, generatedSourcesTest)

    val (classesDir0Main, scalaParamsMain, artifactsMain, projectMain, buildChangedMain) =
      value {
        val res = Build.prepareBuild(
          allInputs,
          sourcesMain,
          generatedSourcesMain,
          options0Main,
          None,
          Scope.Main,
          currentBloopSession.remoteServer,
          persistentLogger,
          buildClient,
          maybeRecoverOnError(mainProjectName)
        )
        res.left.map(_ -> mainProjectName)
      }

    val (classesDir0Test, scalaParamsTest, artifactsTest, projectTest, buildChangedTest) =
      value {
        val res = Build.prepareBuild(
          allInputs,
          sourcesTest,
          generatedSourcesTest,
          options0Test,
          None,
          Scope.Test,
          currentBloopSession.remoteServer,
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
      generatedSourcesMain,
      buildChangedMain
    )

    val testScope = PreBuildData(
      sourcesTest,
      options0Test,
      classesDir0Test,
      scalaParamsTest,
      artifactsTest,
      projectTest,
      generatedSourcesTest,
      buildChangedTest
    )

    if (actionableDiagnostics.getOrElse(true)) {
      val projectOptions = options0Test.orElse(options0Main)
      projectOptions.logActionableDiagnostics(persistentLogger)
    }

    PreBuildModule(module, mainScope, testScope, persistentLogger.diagnostics)
  }

}
