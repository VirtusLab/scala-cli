package scala.build.bsp

import ch.epfl.scala.{bsp4j => b}

import java.util.concurrent.CompletableFuture

trait LoggingBuildServer extends b.BuildServer {
  protected def underlying: b.BuildServer
  override def buildInitialize(
    params: b.InitializeBuildParams
  ): CompletableFuture[b.InitializeBuildResult] =
    underlying.buildInitialize(pprint.stderr.log(params)).logF
  override def onBuildExit(): Unit =
    underlying.onBuildExit()
  override def onBuildInitialized(): Unit =
    underlying.onBuildInitialized()
  override def buildShutdown(): CompletableFuture[Object] =
    underlying.buildShutdown().logF
  override def buildTargetCleanCache(
    params: b.CleanCacheParams
  ): CompletableFuture[b.CleanCacheResult] =
    underlying.buildTargetCleanCache(pprint.stderr.log(params)).logF
  override def buildTargetCompile(params: b.CompileParams): CompletableFuture[b.CompileResult] =
    underlying.buildTargetCompile(pprint.stderr.log(params)).logF
  override def buildTargetDependencySources(
    params: b.DependencySourcesParams
  ): CompletableFuture[b.DependencySourcesResult] =
    underlying.buildTargetDependencySources(pprint.stderr.log(params)).logF
  override def buildTargetInverseSources(
    params: b.InverseSourcesParams
  ): CompletableFuture[b.InverseSourcesResult] =
    underlying.buildTargetInverseSources(pprint.stderr.log(params)).logF
  override def buildTargetResources(
    params: b.ResourcesParams
  ): CompletableFuture[b.ResourcesResult] =
    underlying.buildTargetResources(pprint.stderr.log(params)).logF
  override def buildTargetRun(params: b.RunParams): CompletableFuture[b.RunResult] =
    underlying.buildTargetRun(pprint.stderr.log(params)).logF
  override def buildTargetSources(params: b.SourcesParams): CompletableFuture[b.SourcesResult] =
    underlying.buildTargetSources(pprint.stderr.log(params)).logF
  override def buildTargetTest(params: b.TestParams): CompletableFuture[b.TestResult] =
    underlying.buildTargetTest(pprint.stderr.log(params)).logF
  override def workspaceBuildTargets(): CompletableFuture[b.WorkspaceBuildTargetsResult] =
    underlying.workspaceBuildTargets().logF
  override def workspaceReload(): CompletableFuture[Object] =
    underlying.workspaceReload().logF
}
