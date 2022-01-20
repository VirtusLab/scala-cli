package scala.build.bsp

import ch.epfl.scala.bsp4j.{DependencyModulesParams, DependencyModulesResult}
import ch.epfl.scala.{bsp4j => b}

import java.util.concurrent.CompletableFuture
import java.util.function.BiFunction

trait BuildServerForwardStubs extends b.BuildServer {
  protected def forwardTo: b.BuildServer

  protected def onFatalError(throwable: Throwable): Unit

  def fatalExceptionHandler[T] = new BiFunction[T, Throwable, T] {
    override def apply(maybeValue: T, maybeException: Throwable): T =
      maybeException match {
        case null =>
          maybeValue
        case error =>
          onFatalError(error)
          throw error
      }
  }

  override def buildShutdown(): CompletableFuture[Object] =
    forwardTo.buildShutdown().handleAsync(fatalExceptionHandler)
  override def buildTargetCleanCache(
    params: b.CleanCacheParams
  ): CompletableFuture[b.CleanCacheResult] =
    forwardTo.buildTargetCleanCache(params).handleAsync(fatalExceptionHandler)
  override def buildTargetCompile(params: b.CompileParams): CompletableFuture[b.CompileResult] =
    forwardTo.buildTargetCompile(params).handleAsync(fatalExceptionHandler)
  override def buildTargetDependencySources(
    params: b.DependencySourcesParams
  ): CompletableFuture[b.DependencySourcesResult] =
    forwardTo.buildTargetDependencySources(params).handleAsync(fatalExceptionHandler)
  override def buildTargetInverseSources(
    params: b.InverseSourcesParams
  ): CompletableFuture[b.InverseSourcesResult] =
    forwardTo.buildTargetInverseSources(params).handleAsync(fatalExceptionHandler)
  override def buildTargetResources(
    params: b.ResourcesParams
  ): CompletableFuture[b.ResourcesResult] =
    forwardTo.buildTargetResources(params).handleAsync(fatalExceptionHandler)
  override def buildTargetRun(params: b.RunParams): CompletableFuture[b.RunResult] =
    forwardTo.buildTargetRun(params).handleAsync(fatalExceptionHandler)
  override def buildTargetSources(params: b.SourcesParams): CompletableFuture[b.SourcesResult] =
    forwardTo.buildTargetSources(params).handleAsync(fatalExceptionHandler)
  override def buildTargetTest(params: b.TestParams): CompletableFuture[b.TestResult] =
    forwardTo.buildTargetTest(params).handleAsync(fatalExceptionHandler)
  override def workspaceBuildTargets(): CompletableFuture[b.WorkspaceBuildTargetsResult] =
    forwardTo.workspaceBuildTargets().handleAsync(fatalExceptionHandler)
  override def workspaceReload(): CompletableFuture[Object] =
    forwardTo.workspaceReload().handleAsync(fatalExceptionHandler)
  override def buildTargetDependencyModules(params: DependencyModulesParams)
    : CompletableFuture[DependencyModulesResult] =
    forwardTo.buildTargetDependencyModules(params).handleAsync(fatalExceptionHandler)
}
