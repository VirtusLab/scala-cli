package scala.build.bsp

import ch.epfl.scala.bsp4j as b

import java.util.concurrent.CompletableFuture

import scala.build.GeneratedSource
import scala.build.bsp.buildtargets.{ManagesBuildTargets, ProjectName}
import scala.build.input.{ModuleInputs, compose}
import scala.build.options.Scope

/** A wrapper for [[BspServer]], allowing to reload the workspace on the fly.
  * @param bspServer
  *   the underlying BSP server relying on Bloop
  * @param onReload
  *   the actual `workspace/reload` function
  */
class BuildServerProxy(
  bspServer: () => BspServer,
  onReload: () => CompletableFuture[Object]
) extends b.BuildServer with b.ScalaBuildServer with b.JavaBuildServer with b.JvmBuildServer
    with ScalaScriptBuildServer with ManagesBuildTargets {
  override def buildInitialize(params: b.InitializeBuildParams)
    : CompletableFuture[b.InitializeBuildResult] = bspServer().buildInitialize(params)

  override def onBuildInitialized(): Unit = bspServer().onBuildInitialized()

  override def buildShutdown(): CompletableFuture[AnyRef] = bspServer().buildShutdown()

  override def onBuildExit(): Unit = bspServer().onBuildExit()

  override def workspaceBuildTargets(): CompletableFuture[b.WorkspaceBuildTargetsResult] =
    bspServer().workspaceBuildTargets()

  override def buildTargetSources(params: b.SourcesParams): CompletableFuture[b.SourcesResult] =
    bspServer().buildTargetSources(params)

  override def buildTargetInverseSources(params: b.InverseSourcesParams)
    : CompletableFuture[b.InverseSourcesResult] = bspServer().buildTargetInverseSources(params)

  override def buildTargetDependencySources(params: b.DependencySourcesParams)
    : CompletableFuture[b.DependencySourcesResult] =
    bspServer().buildTargetDependencySources(params)

  override def buildTargetResources(params: b.ResourcesParams)
    : CompletableFuture[b.ResourcesResult] = bspServer().buildTargetResources(params)

  override def buildTargetCompile(params: b.CompileParams): CompletableFuture[b.CompileResult] =
    bspServer().buildTargetCompile(params)

  override def buildTargetTest(params: b.TestParams): CompletableFuture[b.TestResult] =
    bspServer().buildTargetTest(params)

  override def buildTargetRun(params: b.RunParams): CompletableFuture[b.RunResult] =
    bspServer().buildTargetRun(params)

  override def buildTargetCleanCache(params: b.CleanCacheParams)
    : CompletableFuture[b.CleanCacheResult] = bspServer().buildTargetCleanCache(params)

  override def buildTargetDependencyModules(params: b.DependencyModulesParams)
    : CompletableFuture[b.DependencyModulesResult] =
    bspServer().buildTargetDependencyModules(params)

  override def buildTargetScalacOptions(params: b.ScalacOptionsParams)
    : CompletableFuture[b.ScalacOptionsResult] = bspServer().buildTargetScalacOptions(params)

  override def buildTargetScalaTestClasses(params: b.ScalaTestClassesParams)
    : CompletableFuture[b.ScalaTestClassesResult] =
    bspServer().buildTargetScalaTestClasses(params)

  override def buildTargetScalaMainClasses(params: b.ScalaMainClassesParams)
    : CompletableFuture[b.ScalaMainClassesResult] =
    bspServer().buildTargetScalaMainClasses(params)

  override def buildTargetJavacOptions(params: b.JavacOptionsParams)
    : CompletableFuture[b.JavacOptionsResult] = bspServer().buildTargetJavacOptions(params)
  override def debugSessionStart(params: b.DebugSessionParams)
    : CompletableFuture[b.DebugSessionAddress] =
    bspServer().debugSessionStart(params)

  override def buildTargetWrappedSources(params: WrappedSourcesParams)
    : CompletableFuture[WrappedSourcesResult] = bspServer().buildTargetWrappedSources(params)

  override def buildTargetOutputPaths(params: b.OutputPathsParams)
    : CompletableFuture[b.OutputPathsResult] =
    bspServer().buildTargetOutputPaths(params)

  /** As Bloop doesn't support `workspace/reload` requests and we have to reload it on Scala CLI's
    * end, this is used instead of [[BspServer]]'s [[BuildServerForwardStubs]].workspaceReload().
    */
  override def workspaceReload(): CompletableFuture[AnyRef] =
    onReload()

  override def buildTargetJvmRunEnvironment(params: b.JvmRunEnvironmentParams)
    : CompletableFuture[b.JvmRunEnvironmentResult] =
    bspServer().buildTargetJvmRunEnvironment(params)

  override def buildTargetJvmTestEnvironment(params: b.JvmTestEnvironmentParams)
    : CompletableFuture[b.JvmTestEnvironmentResult] =
    bspServer().buildTargetJvmTestEnvironment(params)

  def targetIds: List[b.BuildTargetIdentifier] = bspServer().targetIds
  def targetProjectIdOpt(projectName: ProjectName): Option[b.BuildTargetIdentifier] =
    bspServer().targetProjectIdOpt(projectName)
  def setGeneratedSources(projectName: ProjectName, sources: Seq[GeneratedSource]): Unit =
    bspServer().setGeneratedSources(projectName, sources)
  def addTarget(
    projectName: ProjectName,
    workspace: os.Path,
    scope: Scope,
    generatedSources: Seq[GeneratedSource] = Nil
  ): Unit =
    bspServer().addTarget(projectName, workspace, scope, generatedSources)
  def resetTargets(): Unit =
    bspServer().resetTargets()
  def newInputs(inputs: compose.Inputs): Unit =
    bspServer().newInputs(inputs)
}
