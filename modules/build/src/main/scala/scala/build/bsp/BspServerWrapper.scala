package scala.build.bsp

import scala.build.bloop.ScalaDebugServer
import ch.epfl.scala.{bsp4j => b}

import java.util.concurrent.CompletableFuture
import scala.build.options.Scope
import scala.concurrent.Future

trait BspServerWrapper extends b.BuildServer with b.ScalaBuildServer with b.JavaBuildServer
    with ScalaDebugServer with ScalaScriptBuildServer {
  wrapper: BspServerProxy =>
  override def buildInitialize(params: b.InitializeBuildParams)
    : CompletableFuture[b.InitializeBuildResult] = currentBspServer.buildInitialize(params)

  override def onBuildInitialized(): Unit = currentBspServer.onBuildInitialized()

  override def buildShutdown(): CompletableFuture[AnyRef] = currentBspServer.buildShutdown()

  override def onBuildExit(): Unit = currentBspServer.onBuildExit()

  override def workspaceBuildTargets(): CompletableFuture[b.WorkspaceBuildTargetsResult] =
    currentBspServer.workspaceBuildTargets()

  override def workspaceReload(): CompletableFuture[AnyRef] = currentBspServer.workspaceReload()

  override def buildTargetSources(params: b.SourcesParams): CompletableFuture[b.SourcesResult] =
    currentBspServer.buildTargetSources(params)

  override def buildTargetInverseSources(params: b.InverseSourcesParams)
    : CompletableFuture[b.InverseSourcesResult] = currentBspServer.buildTargetInverseSources(params)

  override def buildTargetDependencySources(params: b.DependencySourcesParams)
    : CompletableFuture[b.DependencySourcesResult] =
    currentBspServer.buildTargetDependencySources(params)

  override def buildTargetResources(params: b.ResourcesParams)
    : CompletableFuture[b.ResourcesResult] = currentBspServer.buildTargetResources(params)

  override def buildTargetCompile(params: b.CompileParams): CompletableFuture[b.CompileResult] =
    currentBspServer.buildTargetCompile(params)

  override def buildTargetTest(params: b.TestParams): CompletableFuture[b.TestResult] =
    currentBspServer.buildTargetTest(params)

  override def buildTargetRun(params: b.RunParams): CompletableFuture[b.RunResult] =
    currentBspServer.buildTargetRun(params)

  override def buildTargetCleanCache(params: b.CleanCacheParams)
    : CompletableFuture[b.CleanCacheResult] = currentBspServer.buildTargetCleanCache(params)

  override def buildTargetDependencyModules(params: b.DependencyModulesParams)
    : CompletableFuture[b.DependencyModulesResult] =
    currentBspServer.buildTargetDependencyModules(params)

  override def buildTargetScalacOptions(params: b.ScalacOptionsParams)
    : CompletableFuture[b.ScalacOptionsResult] = currentBspServer.buildTargetScalacOptions(params)

  override def buildTargetScalaTestClasses(params: b.ScalaTestClassesParams)
    : CompletableFuture[b.ScalaTestClassesResult] =
    currentBspServer.buildTargetScalaTestClasses(params)

  override def buildTargetScalaMainClasses(params: b.ScalaMainClassesParams)
    : CompletableFuture[b.ScalaMainClassesResult] =
    currentBspServer.buildTargetScalaMainClasses(params)

  override def buildTargetJavacOptions(params: b.JavacOptionsParams)
    : CompletableFuture[b.JavacOptionsResult] = currentBspServer.buildTargetJavacOptions(params)

  override def buildTargetDebugSession(params: b.DebugSessionParams)
    : CompletableFuture[b.DebugSessionAddress] = currentBspServer.buildTargetDebugSession(params)

  override def buildTargetWrappedSources(params: WrappedSourcesParams)
    : CompletableFuture[WrappedSourcesResult] = currentBspServer.buildTargetWrappedSources(params)

  override def onConnectWithClient(server: b.BuildClient): Unit =
    currentBspServer.onConnectWithClient(server)

  def targetIds: List[b.BuildTargetIdentifier] = currentBspServer.targetIds
  def targetScopeIdOpt(scope: Scope): Option[b.BuildTargetIdentifier] =
    currentBspServer.targetScopeIdOpt(scope)
  def initiateShutdown: Future[Unit] = currentBspServer.initiateShutdown
}
