package scala.build.bsp

import ch.epfl.scala.bsp4j.{DependencyModulesParams, DependencyModulesResult}
import ch.epfl.scala.{bsp4j => b}

import java.util.concurrent.CompletableFuture
import java.util.function.BiFunction

trait BuildServerForwardStubs extends b.BuildServer {
  protected def forwardTo: b.BuildServer

  protected def onFatalError(throwable: Throwable, context: String): Unit

  def fatalExceptionHandler[T](methodName: String, params: Any*) = new BiFunction[T, Throwable, T] {
    override def apply(maybeValue: T, maybeException: Throwable): T =
      maybeException match {
        case null =>
          maybeValue
        case error =>
          val methodContext = s"bloop bsp server, method: $methodName"
          val context =
            if (params.isEmpty) methodContext
            else
              params.mkString(s"$methodContext, with params: ", ", ", "")
          onFatalError(error, context)
          throw error
      }
  }

  override def buildShutdown(): CompletableFuture[Object] =
    forwardTo.buildShutdown().handleAsync(fatalExceptionHandler("buildShutdown"))

  override def buildTargetCleanCache(
    params: b.CleanCacheParams
  ): CompletableFuture[b.CleanCacheResult] =
    forwardTo.buildTargetCleanCache(params)
      .handleAsync(fatalExceptionHandler("buildTargetCleanCache", params))

  override def buildTargetCompile(params: b.CompileParams): CompletableFuture[b.CompileResult] =
    forwardTo.buildTargetCompile(params)
      .handleAsync(fatalExceptionHandler("buildTargetCompile", params))

  override def buildTargetDependencySources(
    params: b.DependencySourcesParams
  ): CompletableFuture[b.DependencySourcesResult] =
    forwardTo.buildTargetDependencySources(params)
      .handleAsync(fatalExceptionHandler("buildTargetDependencySources", params))

  override def buildTargetInverseSources(
    params: b.InverseSourcesParams
  ): CompletableFuture[b.InverseSourcesResult] =
    forwardTo.buildTargetInverseSources(params)
      .handleAsync(fatalExceptionHandler("buildTargetInverseSources", params))

  override def buildTargetResources(
    params: b.ResourcesParams
  ): CompletableFuture[b.ResourcesResult] =
    forwardTo.buildTargetResources(params)
      .handleAsync(fatalExceptionHandler("buildTargetResources", params))

  override def buildTargetRun(params: b.RunParams): CompletableFuture[b.RunResult] =
    forwardTo.buildTargetRun(params)
      .handleAsync(fatalExceptionHandler("buildTargetRun", params))

  override def buildTargetSources(params: b.SourcesParams): CompletableFuture[b.SourcesResult] =
    forwardTo.buildTargetSources(params)
      .handleAsync(fatalExceptionHandler("buildTargetSources", params))

  override def buildTargetTest(params: b.TestParams): CompletableFuture[b.TestResult] =
    forwardTo.buildTargetTest(params)
      .handleAsync(fatalExceptionHandler("buildTargetTest", params))

  override def workspaceBuildTargets(): CompletableFuture[b.WorkspaceBuildTargetsResult] =
    forwardTo.workspaceBuildTargets()
      .handleAsync(fatalExceptionHandler("workspaceBuildTargets"))

  override def workspaceReload(): CompletableFuture[Object] =
    CompletableFuture.completedFuture(new Object)
  // Bloop does not support workspaceReload and Intellij calls it at the start
  // forwardTo.workspaceReload()
  //   .handleAsync(fatalExceptionHandler("workspaceReload"))

  override def buildTargetDependencyModules(params: DependencyModulesParams)
    : CompletableFuture[DependencyModulesResult] =
    forwardTo.buildTargetDependencyModules(params)
      .handleAsync(fatalExceptionHandler("buildTargetDependencyModules", params))
}
