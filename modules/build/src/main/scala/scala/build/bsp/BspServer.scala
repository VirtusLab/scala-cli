package scala.build.bsp

import _root_.bloop.config.{Config, ConfigCodecs => BloopCodecs}
import ch.epfl.scala.bsp4j.{BuildClient, BuildTargetEvent, LogMessageParams, MessageType}
import ch.epfl.scala.{bsp4j => b}
import com.github.plokhotnyuk.jsoniter_scala.core

import java.io.{File, PrintWriter, StringWriter}
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{CompletableFuture, TimeUnit}
import java.{util => ju}

import scala.build.Logger
import scala.build.bloop.{ScalaDebugServer, ScalaDebugServerForwardStubs}
import scala.build.internal.Constants
import scala.build.options.Scope
import scala.collection.concurrent
import scala.concurrent.{Future, Promise}
import scala.jdk.CollectionConverters.*
import scala.util.{Random, Try}

class BspServer(
  bloopServer: b.BuildServer with b.ScalaBuildServer with b.JavaBuildServer with ScalaDebugServer,
  compile: (() => CompletableFuture[b.CompileResult]) => CompletableFuture[b.CompileResult],
  logger: Logger
) extends b.BuildServer with b.ScalaBuildServer with b.JavaBuildServer with BuildServerForwardStubs
    with ScalaScriptBuildServer
    with ScalaDebugServerForwardStubs
    with ScalaBuildServerForwardStubs with JavaBuildServerForwardStubs with HasGeneratedSources {

  private var client: Option[BuildClient]                           = None
  private val isIntelliJ: AtomicBoolean                             = new AtomicBoolean(false)
  private val buildTargetNamesByUri: concurrent.Map[String, String] = concurrent.TrieMap.empty

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

  private def mapGeneratedSources(res: b.SourcesResult): Unit = {
    val gen = generatedSources.values.toVector
    for {
      item <- res.getItems().asScala
      if validTarget(item.getTarget)
      sourceItem <- item.getSources.asScala
      genSource  <- gen.iterator.flatMap(_.uriMap.get(sourceItem.getUri).iterator).take(1)
      updatedUri <- genSource.reportingPath.toOption.map(_.toNIO.toUri.toASCIIString)
    } {
      sourceItem.setUri(updatedUri)
      sourceItem.setGenerated(false)
    }
  }

  protected def forwardTo = bloopServer

  private val supportedLanguages: ju.List[String] = List("scala", "java").asJava

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
      scala.build.blooprifle.internal.Constants.bspVersion,
      capabilities
    )
    val buildComesFromIntelliJ = params.getDisplayName.toLowerCase.contains("intellij")
    isIntelliJ.set(buildComesFromIntelliJ)
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
      val updatedItems = res.getItems().asScala.map {
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
          isIntelliJ.get() && baseDirectory.getName == ".scala-build" && baseDirectory.getParentFile != null
        ) {
          val newBaseDirectory = baseDirectory.getParentFile.toPath.toUri.toASCIIString
          target.setBaseDirectory(newBaseDirectory)
        }
        buildTargetNamesByUri.put(target.getId.getUri, target.getDisplayName)
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

  // TODO: reload dependencies
  // TODO: reload scalac & javac options
  // TODO: fix IntelliJ run tasks not picking up new sources
  override def workspaceReload(): CompletableFuture[Object] =
    super.workspaceReload().thenApply[Object] { res =>
      val buildTargetsToRecompile: List[b.BuildTargetIdentifier] =
        for { // this is just a workaround hack to emulate workspace reloading until Bloop itself supports it
          case (targetUri, targetName) <- buildTargetNamesByUri.toList
          scalaBuildDirPath = os.Path(new URI(targetUri).getRawPath)
          if os.isDir(scalaBuildDirPath)
          ideInputsJsonPath = scalaBuildDirPath / "ide-inputs.json"
          if os.isFile(ideInputsJsonPath)
          bloopDirPath = scalaBuildDirPath / ".bloop"
          if os.isDir(bloopDirPath)
          bloopFileJsonPath = bloopDirPath / s"$targetName.json"
          if os.isFile(bloopFileJsonPath)
          bloopFile <-
            Try(core.readFromString(os.read(bloopFileJsonPath))(BloopCodecs.codecFile)).toOption
          ideInputs <-
            Try(core.readFromString(os.read(ideInputsJsonPath))(IdeInputs.codec)).toOption
          ideSources =
            if (targetName.endsWith("-test")) ideInputs.testScopeSources
            else ideInputs.mainScopeSources
          ideInputPaths = ideSources.map(os.Path(_).toNIO)
          if !bloopFile.project.sources.toSet.equals(ideInputPaths.toSet)
        } yield {
          val updatedBloopFile: Config.File =
            bloopFile.copy(project = bloopFile.project.copy(sources = ideInputPaths))
          val updatedBloopFileJson: Array[Byte] =
            core.writeToArray(updatedBloopFile)(BloopCodecs.codecFile)
          os.write.over(bloopFileJsonPath, updatedBloopFileJson, createFolders = true)
          new b.BuildTargetIdentifier(targetUri)
        }
      if (buildTargetsToRecompile.nonEmpty) {
        val buildTargetCompileParams = new b.CompileParams(buildTargetsToRecompile.asJava)
        bloopServer.buildTargetCompile(buildTargetCompileParams).thenApply { compileRes =>
          val buildTargetEvents: ju.List[BuildTargetEvent] = buildTargetsToRecompile.map { bt =>
            val event = new b.BuildTargetEvent(bt)
            event.setKind(b.BuildTargetEventKind.CHANGED)
            event
          }.asJava
          val buildTargetChangedParams = new b.DidChangeBuildTarget(buildTargetEvents)
          client.foreach(_.onBuildTargetDidChange(buildTargetChangedParams))
          compileRes
        }
      }
      res // TODO: return a valid json-rpc error message in case of any failures
    }

  def initiateShutdown: Future[Unit] =
    shutdownPromise.future
}
