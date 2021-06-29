package scala.build.bsp

import ch.epfl.scala.{bsp4j => b}

import java.util.concurrent.CompletableFuture

trait BuildServerForwardStubs extends b.BuildServer {
  protected def forwardTo: b.BuildServer
  override def buildShutdown(): CompletableFuture[Object] =
    forwardTo.buildShutdown()
  override def buildTargetCleanCache(params: b.CleanCacheParams): CompletableFuture[b.CleanCacheResult] =
    forwardTo.buildTargetCleanCache(params)
  override def buildTargetCompile(params: b.CompileParams): CompletableFuture[b.CompileResult] =
    forwardTo.buildTargetCompile(params)
  override def buildTargetDependencySources(params: b.DependencySourcesParams): CompletableFuture[b.DependencySourcesResult] =
    forwardTo.buildTargetDependencySources(params)
  override def buildTargetInverseSources(params: b.InverseSourcesParams): CompletableFuture[b.InverseSourcesResult] =
    forwardTo.buildTargetInverseSources(params)
  override def buildTargetResources(params: b.ResourcesParams): CompletableFuture[b.ResourcesResult] =
    forwardTo.buildTargetResources(params)
  override def buildTargetRun(params: b.RunParams): CompletableFuture[b.RunResult] =
    forwardTo.buildTargetRun(params)
  override def buildTargetSources(params: b.SourcesParams): CompletableFuture[b.SourcesResult] =
    forwardTo.buildTargetSources(params)
  override def buildTargetTest(params: b.TestParams): CompletableFuture[b.TestResult] =
    forwardTo.buildTargetTest(params)
  override def workspaceBuildTargets(): CompletableFuture[b.WorkspaceBuildTargetsResult] =
    forwardTo.workspaceBuildTargets()
  override def workspaceReload(): CompletableFuture[Object] =
    forwardTo.workspaceReload()
}
