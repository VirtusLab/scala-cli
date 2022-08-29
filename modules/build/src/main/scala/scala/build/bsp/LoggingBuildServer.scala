package scala.build.bsp

import ch.epfl.scala.bsp4j.{DependencyModulesParams, DependencyModulesResult}
import ch.epfl.scala.{bsp4j => b}

import java.util.concurrent.CompletableFuture

trait LoggingBuildServer extends b.BuildServer {
  protected def underlying: b.BuildServer
  override def buildInitialize(
    params: b.InitializeBuildParams
  ): CompletableFuture[b.InitializeBuildResult] =
    underlying.buildInitialize(pprint.err.log(params)).logF
  override def onBuildExit(): Unit =
    underlying.onBuildExit()
  override def onBuildInitialized(): Unit =
    underlying.onBuildInitialized()
  override def buildShutdown(): CompletableFuture[Object] =
    underlying.buildShutdown().logF
  override def buildTargetCleanCache(
    params: b.CleanCacheParams
  ): CompletableFuture[b.CleanCacheResult] =
    underlying.buildTargetCleanCache(pprint.err.log(params)).logF
  override def buildTargetCompile(params: b.CompileParams): CompletableFuture[b.CompileResult] =
    underlying.buildTargetCompile(pprint.err.log(params)).logF
  override def buildTargetDependencySources(
    params: b.DependencySourcesParams
  ): CompletableFuture[b.DependencySourcesResult] =
    underlying.buildTargetDependencySources(pprint.err.log(params)).logF
  override def buildTargetInverseSources(
    params: b.InverseSourcesParams
  ): CompletableFuture[b.InverseSourcesResult] =
    underlying.buildTargetInverseSources(pprint.err.log(params)).logF
  override def buildTargetResources(
    params: b.ResourcesParams
  ): CompletableFuture[b.ResourcesResult] =
    underlying.buildTargetResources(pprint.err.log(params)).logF
  override def buildTargetRun(params: b.RunParams): CompletableFuture[b.RunResult] =
    underlying.buildTargetRun(pprint.err.log(params)).logF
  override def buildTargetSources(params: b.SourcesParams): CompletableFuture[b.SourcesResult] =
    underlying.buildTargetSources(pprint.err.log(params)).logF
  override def buildTargetTest(params: b.TestParams): CompletableFuture[b.TestResult] =
    underlying.buildTargetTest(pprint.err.log(params)).logF
  override def debugSessionStart(params: b.DebugSessionParams)
    : CompletableFuture[b.DebugSessionAddress] =
    underlying.debugSessionStart(pprint.err.log(params)).logF
  override def workspaceBuildTargets(): CompletableFuture[b.WorkspaceBuildTargetsResult] =
    underlying.workspaceBuildTargets().logF
  override def workspaceReload(): CompletableFuture[Object] =
    underlying.workspaceReload().logF
  override def buildTargetDependencyModules(params: DependencyModulesParams)
    : CompletableFuture[DependencyModulesResult] =
    underlying.buildTargetDependencyModules(pprint.err.log(params)).logF
}
