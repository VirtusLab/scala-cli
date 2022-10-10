package scala.build.bsp

import ch.epfl.scala.{bsp4j => b}

import java.util.concurrent.CompletableFuture

import scala.build.options.Scope
import scala.build.{GeneratedSource, Inputs}

class BuildServerProxy(
  bspServer: () => BspServer,
  onReload: () => CompletableFuture[Object]
) extends b.BuildServer with b.ScalaBuildServer with b.JavaBuildServer
    with ScalaScriptBuildServer with HasGeneratedSources {
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
  override def workspaceReload(): CompletableFuture[AnyRef] =
    onReload()

  override def onConnectWithClient(server: b.BuildClient): Unit =
    bspServer().onConnectWithClient(server)

  def targetIds: List[b.BuildTargetIdentifier] = bspServer().targetIds
  def targetScopeIdOpt(scope: Scope): Option[b.BuildTargetIdentifier] =
    bspServer().targetScopeIdOpt(scope)
  def setGeneratedSources(scope: Scope, sources: Seq[GeneratedSource]): Unit =
    bspServer().setGeneratedSources(scope, sources)
  def setProjectName(workspace: os.Path, name: String, scope: Scope): Unit =
    bspServer().setProjectName(workspace, name, scope)
  def resetProjectNames(): Unit =
    bspServer().resetProjectNames()
  def newInputs(inputs: Inputs): Unit =
    bspServer().newInputs(inputs)
}
