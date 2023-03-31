package scala.build.bsp

import ch.epfl.scala.bsp4j.{BuildClient, LogMessageParams, MessageType}
import ch.epfl.scala.bsp4j as b

import java.io.{File, PrintWriter, StringWriter}
import java.net.URI
import java.util.concurrent.{CompletableFuture, TimeUnit}
import java.util as ju

import scala.build.Logger
import scala.build.internal.Constants
import scala.build.options.Scope
import scala.concurrent.{Future, Promise}
import scala.jdk.CollectionConverters.*
import scala.util.Random

class BspServer(
  bloopServer: b.BuildServer & b.ScalaBuildServer & b.JavaBuildServer & b.JvmBuildServer,
  compile: (() => CompletableFuture[b.CompileResult]) => CompletableFuture[b.CompileResult],
  logger: Logger,
  presetIntelliJ: Boolean = false
) extends b.BuildServer with b.ScalaBuildServer with b.JavaBuildServer with BuildServerForwardStubs
    with ScalaScriptBuildServer
    with ScalaBuildServerForwardStubs
    with JavaBuildServerForwardStubs
    with JvmBuildServerForwardStubs
    with HasGeneratedSourcesImpl {

  private var client: Option[BuildClient] = None

  @volatile private var intelliJ: Boolean = presetIntelliJ
  def isIntelliJ: Boolean                 = intelliJ

  def clientOpt: Option[BuildClient] = client

  override def onConnectWithClient(client: BuildClient): Unit = this.client = Some(client)

  private var extraDependencySources: Seq[os.Path] = Nil
  def setExtraDependencySources(sourceJars: Seq[os.Path]): Unit = {
    extraDependencySources = sourceJars
  }

  // Can we accept some errors in some circumstances?
  override protected def onFatalError(throwable: Throwable, context: String): Unit = {
    val sw = new StringWriter()
    throwable.printStackTrace(new PrintWriter(sw))
    val message =
      s"Fatal error has occured within $context. Shutting down the server:\n ${sw.toString}"
    System.err.println(message)
    client.foreach(_.onBuildLogMessage(new LogMessageParams(MessageType.ERROR, message)))

    // wait random bit before shutting down server to reduce risk of multiple scala-cli instances starting bloop at the same time
    val timeout = Random.nextInt(400)
    TimeUnit.MILLISECONDS.sleep(100 + timeout)
    sys.exit(1)
  }

  private def maybeUpdateProjectTargetUri(res: b.WorkspaceBuildTargetsResult): Unit =
    for {
      (_, n) <- projectNames.iterator
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
  private def check(params: b.DebugSessionParams): params.type = {
    val invalidTargets = params.getTargets.asScala.filter(!validTarget(_))
    for (target <- invalidTargets)
      logger.debug(s"invalid target in Test request: $target")
    params
  }
  private def check(params: b.OutputPathsParams): params.type = {
    val invalidTargets = params.getTargets.asScala.filter(!validTarget(_))
    for (target <- invalidTargets)
      logger.debug(s"invalid target in buildTargetOutputPaths request: $target")
    params
  }
  private def mapGeneratedSources(res: b.SourcesResult): Unit = {
    val gen = generatedSources.values.toVector
    for {
      item <- res.getItems.asScala
      if validTarget(item.getTarget)
      sourceItem <- item.getSources.asScala
      genSource  <- gen.iterator.flatMap(_.uriMap.get(sourceItem.getUri).iterator).take(1)
      updatedUri <- genSource.reportingPath.toOption.map(_.toNIO.toUri.toASCIIString)
    } {
      sourceItem.setUri(updatedUri)
      sourceItem.setGenerated(false)
    }
  }

  protected def forwardTo
    : b.BuildServer & b.ScalaBuildServer & b.JavaBuildServer & b.JvmBuildServer = bloopServer

  private val supportedLanguages: ju.List[String] = List(
    "scala",
    "java",
    // This makes Metals requests "wrapped sources" stuff, that makes it handle .sc files better.
    "scala-sc"
  ).asJava

  private def capabilities: b.BuildServerCapabilities = {
    val capabilities = new b.BuildServerCapabilities
    capabilities.setCompileProvider(new b.CompileProvider(supportedLanguages))
    capabilities.setTestProvider(new b.TestProvider(supportedLanguages))
    capabilities.setRunProvider(new b.RunProvider(supportedLanguages))
    capabilities.setDebugProvider(new b.DebugProvider(supportedLanguages))
    capabilities.setInverseSourcesProvider(true)
    capabilities.setDependencySourcesProvider(true)
    capabilities.setResourcesProvider(true)
    capabilities.setBuildTargetChangedProvider(true)
    capabilities.setJvmRunEnvironmentProvider(true)
    capabilities.setJvmTestEnvironmentProvider(true)
    capabilities.setCanReload(true)
    capabilities.setDependencyModulesProvider(true)
    capabilities
  }

  override def buildInitialize(
    params: b.InitializeBuildParams
  ): CompletableFuture[b.InitializeBuildResult] = {
    val res = new b.InitializeBuildResult(
      "scala-cli",
      Constants.version,
      bloop.rifle.internal.Constants.bspVersion,
      capabilities
    )
    val buildComesFromIntelliJ = params.getDisplayName.toLowerCase.contains("intellij")
    intelliJ = buildComesFromIntelliJ
    logger.debug(s"IntelliJ build: $buildComesFromIntelliJ")
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
        s"Got invalid target in Run request: ${target.getUri} (expected ${targetScopeIdOpt(Scope.Main).orNull})"
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

  override def debugSessionStart(params: b.DebugSessionParams)
    : CompletableFuture[b.DebugSessionAddress] =
    super.debugSessionStart(check(params))

  override def buildTargetOutputPaths(params: b.OutputPathsParams)
    : CompletableFuture[b.OutputPathsResult] = {
    check(params)
    val targets = params.getTargets.asScala.filter(validTarget)
    val outputPathsItem =
      targets
        .map(buildTargetId => (buildTargetId, targetWorkspaceDirOpt(buildTargetId)))
        .collect { case (buildTargetId, Some(targetUri)) => (buildTargetId, targetUri) }
        .map {
          case (buildTargetId, targetUri) =>
            new b.OutputPathsItem(
              buildTargetId,
              List(b.OutputPathItem(targetUri, b.OutputPathItemKind.DIRECTORY)).asJava
            )
        }

    CompletableFuture.completedFuture(new b.OutputPathsResult(outputPathsItem.asJava))
  }

  override def workspaceBuildTargets(): CompletableFuture[b.WorkspaceBuildTargetsResult] =
    super.workspaceBuildTargets().thenApply { res =>
      maybeUpdateProjectTargetUri(res)
      val res0 = res.duplicate()
      stripInvalidTargets(res0)
      for (target <- res0.getTargets.asScala) {
        val capabilities = target.getCapabilities
        capabilities.setCanDebug(true)
        val baseDirectory = new File(new URI(target.getBaseDirectory))
        if (
          isIntelliJ && baseDirectory.getName == Constants.workspaceDirName && baseDirectory.getParentFile != null
        ) {
          val newBaseDirectory = baseDirectory.getParentFile.toPath.toUri.toASCIIString
          target.setBaseDirectory(newBaseDirectory)
        }
      }
      res0
    }

  def buildTargetWrappedSources(params: WrappedSourcesParams)
    : CompletableFuture[WrappedSourcesResult] = {
    def sourcesItemOpt(scope: Scope) = targetScopeIdOpt(scope).map { id =>
      val items = generatedSources
        .getOrElse(scope, HasGeneratedSources.GeneratedSources(Nil))
        .sources
        .flatMap { s =>
          s.reportingPath.toSeq.map(_.toNIO.toUri.toASCIIString).map { uri =>
            val item    = new WrappedSourceItem(uri, s.generated.toNIO.toUri.toASCIIString)
            val content = os.read(s.generated)
            item.setTopWrapper(content.take(s.topWrapperLen))
            item.setBottomWrapper("}") // meh
            item
          }
        }
      new WrappedSourcesItem(id, items.asJava)
    }
    val sourceItems = Seq(Scope.Main, Scope.Test).flatMap(sourcesItemOpt(_).toSeq)
    val res         = new WrappedSourcesResult(sourceItems.asJava)
    CompletableFuture.completedFuture(res)
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
