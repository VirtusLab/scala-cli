package scala.build.bsp

import ch.epfl.scala.bsp4j as b
import ch.epfl.scala.bsp4j.{DependencyModulesParams, DependencyModulesResult}

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
    forwardTo.buildShutdown().handle(fatalExceptionHandler("buildShutdown"))

  override def buildTargetCleanCache(
    params: b.CleanCacheParams
  ): CompletableFuture[b.CleanCacheResult] =
    forwardTo.buildTargetCleanCache(params)
      .handle(fatalExceptionHandler("buildTargetCleanCache", params))

  override def buildTargetCompile(params: b.CompileParams): CompletableFuture[b.CompileResult] =
    forwardTo.buildTargetCompile(params)
      .handle(fatalExceptionHandler("buildTargetCompile", params))

  override def buildTargetDependencySources(
    params: b.DependencySourcesParams
  ): CompletableFuture[b.DependencySourcesResult] =
    forwardTo.buildTargetDependencySources(params)
      .handle(fatalExceptionHandler("buildTargetDependencySources", params))

  override def buildTargetInverseSources(
    params: b.InverseSourcesParams
  ): CompletableFuture[b.InverseSourcesResult] =
    forwardTo.buildTargetInverseSources(params)
      .handle(fatalExceptionHandler("buildTargetInverseSources", params))

  override def buildTargetResources(
    params: b.ResourcesParams
  ): CompletableFuture[b.ResourcesResult] =
    forwardTo.buildTargetResources(params)
      .handle(fatalExceptionHandler("buildTargetResources", params))

  override def buildTargetRun(params: b.RunParams): CompletableFuture[b.RunResult] =
    forwardTo.buildTargetRun(params)
      .handle(fatalExceptionHandler("buildTargetRun", params))

  override def buildTargetSources(params: b.SourcesParams): CompletableFuture[b.SourcesResult] =
    forwardTo.buildTargetSources(params)
      .handle(fatalExceptionHandler("buildTargetSources", params))

  override def buildTargetTest(params: b.TestParams): CompletableFuture[b.TestResult] =
    forwardTo.buildTargetTest(params)
      .handle(fatalExceptionHandler("buildTargetTest", params))

  override def debugSessionStart(params: b.DebugSessionParams)
    : CompletableFuture[b.DebugSessionAddress] =
    forwardTo.debugSessionStart(params)
      .handle(fatalExceptionHandler("debugSessionStart", params))

  override def workspaceBuildTargets(): CompletableFuture[b.WorkspaceBuildTargetsResult] =
    forwardTo.workspaceBuildTargets()
      .handle(fatalExceptionHandler("workspaceBuildTargets"))

  /** This implementation should never be called and is merely a placeholder. As Bloop doesn't
    * support reloading its workspace, Scala CLI has to reload Bloop instead. And so,
    * [[BuildServerProxy]].workspaceReload() is responsible for the actual reload.
    */
  override def workspaceReload(): CompletableFuture[Object] =
    CompletableFuture.completedFuture(new Object) // should never be called, as per scaladoc

  override def buildTargetDependencyModules(params: DependencyModulesParams)
    : CompletableFuture[DependencyModulesResult] =
    forwardTo.buildTargetDependencyModules(params)
      .handle(fatalExceptionHandler("buildTargetDependencyModules", params))
}
