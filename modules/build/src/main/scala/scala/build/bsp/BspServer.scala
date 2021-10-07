package scala.build.bsp

import ch.epfl.scala.{bsp4j => b}

import java.util.concurrent.CompletableFuture

import scala.build.Logger
import scala.build.internal.Constants
import scala.collection.JavaConverters._
import scala.concurrent.{Future, Promise}

class BspServer(
  bloopServer: b.BuildServer with b.ScalaBuildServer with b.JavaBuildServer,
  compile: (() => CompletableFuture[b.CompileResult]) => CompletableFuture[b.CompileResult],
  logger: Logger
) extends b.BuildServer with b.ScalaBuildServer with b.JavaBuildServer with BuildServerForwardStubs
    with ScalaBuildServerForwardStubs with JavaBuildServerForwardStubs with HasGeneratedSources {

  private var extraDependencySources: Seq[os.Path] = Nil
  def setExtraDependencySources(sourceJars: Seq[os.Path]): Unit = {
    extraDependencySources = sourceJars
  }

  private def maybeUpdateProjectTargetUri(res: b.WorkspaceBuildTargetsResult): Unit =
    for {
      n <- projectNameOpt.iterator
      if n.targetUriOpt.isEmpty
      target <- res.getTargets.asScala.iterator.find(_.getDisplayName == n.name)
    } n.targetUriOpt = Some(target.getId.getUri)

  private def stripInvalidTargets(params: b.WorkspaceBuildTargetsResult): Unit = {
    val updatedTargets = params
      .getTargets
      .asScala
      .filter(target => validTarget(target.getId))
      .asJava
    params.setTargets(updatedTargets)
  }

  private def check(params: b.CleanCacheParams): params.type = {
    val invalidTargets = params.getTargets.asScala.filter(!validTarget(_))
    for (target <- invalidTargets)
      logger.debug(s"invalid target in CleanCache request: $target")
    params
  }
  private def check(params: b.CompileParams): params.type = {
    val invalidTargets = params.getTargets.asScala.filter(!validTarget(_))
    for (target <- invalidTargets)
      logger.debug(s"invalid target in Compile request: $target")
    params
  }
  private def check(params: b.DependencySourcesParams): params.type = {
    val invalidTargets = params.getTargets.asScala.filter(!validTarget(_))
    for (target <- invalidTargets)
      logger.debug(s"invalid target in DependencySources request: $target")
    params
  }
  private def check(params: b.ResourcesParams): params.type = {
    val invalidTargets = params.getTargets.asScala.filter(!validTarget(_))
    for (target <- invalidTargets)
      logger.debug(s"invalid target in Resources request: $target")
    params
  }
  private def check(params: b.SourcesParams): params.type = {
    val invalidTargets = params.getTargets.asScala.filter(!validTarget(_))
    for (target <- invalidTargets)
      logger.debug(s"invalid target in Sources request: $target")
    params
  }
  private def check(params: b.TestParams): params.type = {
    val invalidTargets = params.getTargets.asScala.filter(!validTarget(_))
    for (target <- invalidTargets)
      logger.debug(s"invalid target in Test request: $target")
    params
  }

  private def mapGeneratedSources(res: b.SourcesResult): Unit = {
    val gen = generatedSources
    for {
      item <- res.getItems.asScala
      if validTarget(item.getTarget)
      sourceItem <- item.getSources.asScala
      genSource  <- gen.uriMap.get(sourceItem.getUri)
      updatedUri <- genSource.reportingPath.toOption.map(_.toNIO.toUri.toASCIIString)
    } {
      sourceItem.setUri(updatedUri)
      sourceItem.setGenerated(false)
    }
  }

  protected def forwardTo = bloopServer

  private def capabilities: b.BuildServerCapabilities =
    new b.BuildServerCapabilities

  override def buildInitialize(
    params: b.InitializeBuildParams
  ): CompletableFuture[b.InitializeBuildResult] = {
    val res = new b.InitializeBuildResult(
      "scala-cli",
      Constants.version,
      scala.build.blooprifle.internal.Constants.bspVersion,
      capabilities
    )
    CompletableFuture.completedFuture(res)
  }

  override def onBuildInitialized(): Unit = ()

  override def buildTargetCleanCache(
    params: b.CleanCacheParams
  ): CompletableFuture[b.CleanCacheResult] =
    super.buildTargetCleanCache(check(params))

  override def buildTargetCompile(params: b.CompileParams): CompletableFuture[b.CompileResult] =
    compile(() => super.buildTargetCompile(check(params)))

  override def buildTargetDependencySources(
    params: b.DependencySourcesParams
  ): CompletableFuture[b.DependencySourcesResult] =
    super.buildTargetDependencySources(check(params)).thenApply { res =>
      val updatedItems = res.getItems.asScala.map {
        case item if validTarget(item.getTarget) =>
          val updatedSources = item.getSources.asScala ++ extraDependencySources.map { sourceJar =>
            sourceJar.toNIO.toUri.toASCIIString
          }
          new b.DependencySourcesItem(item.getTarget, updatedSources.asJava)
        case other => other
      }

      new b.DependencySourcesResult(updatedItems.asJava)
    }

  override def buildTargetResources(
    params: b.ResourcesParams
  ): CompletableFuture[b.ResourcesResult] =
    super.buildTargetResources(check(params))

  override def buildTargetRun(params: b.RunParams): CompletableFuture[b.RunResult] = {
    val target = params.getTarget
    if (!validTarget(target))
      logger.debug(
        s"Got invalid target in Run request: ${target.getUri} (expected ${projectNameOpt.flatMap(_.targetUriOpt).orNull})"
      )
    super.buildTargetRun(params)
  }

  override def buildTargetSources(params: b.SourcesParams): CompletableFuture[b.SourcesResult] =
    super.buildTargetSources(check(params)).thenApply { res =>
      val res0 = res.duplicate()
      mapGeneratedSources(res0)
      res0
    }

  override def buildTargetTest(params: b.TestParams): CompletableFuture[b.TestResult] =
    super.buildTargetTest(check(params))

  override def workspaceBuildTargets(): CompletableFuture[b.WorkspaceBuildTargetsResult] =
    super.workspaceBuildTargets().thenApply { res =>
      maybeUpdateProjectTargetUri(res)
      val res0 = res.duplicate()
      stripInvalidTargets(res0)
      for (target <- res0.getTargets.asScala) {
        val capabilities = target.getCapabilities
        // TODO Re-enable once we support this via BSP
        capabilities.setCanRun(false)
        capabilities.setCanTest(false)
      }
      res0
    }

  private val shutdownPromise = Promise[Unit]()
  override def buildShutdown(): CompletableFuture[Object] = {
    if (!shutdownPromise.isCompleted)
      shutdownPromise.success(())
    CompletableFuture.completedFuture(null)
  }

  override def onBuildExit(): Unit = ()

  def initiateShutdown: Future[Unit] =
    shutdownPromise.future
}
