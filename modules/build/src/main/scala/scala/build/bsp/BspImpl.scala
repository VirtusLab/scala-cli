package scala.build.bsp

import ch.epfl.scala.{bsp4j => b}
import com.swoval.files.PathWatchers
import org.eclipse.lsp4j.jsonrpc

import java.io.{InputStream, OutputStream}
import java.util.concurrent.{CompletableFuture, Executor}

import scala.build.EitherCps.{either, value}
import scala.build.*
import scala.build.blooprifle.BloopRifleConfig
import scala.build.errors.BuildException
import scala.build.internal.CustomCodeWrapper
import scala.build.options.{BuildOptions, Scope}
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success}

final class BspImpl(
  logger: Logger,
  bloopRifleConfig: BloopRifleConfig,
  initialInputs: Inputs,
  argsToInputs: Seq[String] => Either[String, Inputs],
  buildOptions: BuildOptions,
  verbosity: Int,
  threads: BspThreads,
  in: InputStream,
  out: OutputStream
) extends Bsp {
  def notifyBuildChange(bspServerProxy: BspServerProxy): Unit = {
    val events =
      for (targetId <- bspServerProxy.targetIds)
        yield {
          val event = new b.BuildTargetEvent(targetId)
          event.setKind(b.BuildTargetEventKind.CHANGED)
          event
        }
    val params = new b.DidChangeBuildTarget(events.asJava)
    actualLocalClient.onBuildTargetDidChange(params)
  }

  private def prepareBuild(bspServerProxy: BspServerProxy)
    : Either[(BuildException, Scope), PreBuildProject] = either {
    logger.log("Preparing build")

    val persistentLogger = new PersistentDiagnosticLogger(logger)
    val currentBspServer = bspServerProxy.currentBspServer
    val inputs           = currentBspServer.inputs

    val crossSources = value {
      CrossSources.forInputs(
        inputs,
        Sources.defaultPreprocessors(
          buildOptions.scriptOptions.codeWrapper.getOrElse(CustomCodeWrapper)
        ),
        persistentLogger
      ).left.map((_, Scope.Main))
    }

    if (verbosity >= 3)
      pprint.err.log(crossSources)

    val scopedSources = value(crossSources.scopedSources(buildOptions))

    if (verbosity >= 3)
      pprint.err.log(scopedSources)

    val sourcesMain = scopedSources.sources(Scope.Main, crossSources.sharedOptions(buildOptions))
    val sourcesTest = scopedSources.sources(Scope.Test, crossSources.sharedOptions(buildOptions))

    if (verbosity >= 3)
      pprint.err.log(sourcesMain)

    val options0Main = sourcesMain.buildOptions
    val options0Test = sourcesTest.buildOptions.orElse(options0Main)

    val generatedSourcesMain =
      sourcesMain.generateSources(inputs.generatedSrcRoot(Scope.Main))
    val generatedSourcesTest =
      sourcesTest.generateSources(inputs.generatedSrcRoot(Scope.Test))

    currentBspServer.setExtraDependencySources(buildOptions.classPathOptions.extraSourceJars)
    currentBspServer.setGeneratedSources(Scope.Main, generatedSourcesMain)
    currentBspServer.setGeneratedSources(Scope.Test, generatedSourcesTest)

    val (classesDir0Main, scalaParamsMain, artifactsMain, projectMain, buildChangedMain) = value {
      val res = Build.prepareBuild(
        inputs,
        sourcesMain,
        generatedSourcesMain,
        options0Main,
        None,
        Scope.Main,
        bspServerProxy.currentBloopCompiler,
        persistentLogger
      )
      res.left.map((_, Scope.Main))
    }

    val (classesDir0Test, scalaParamsTest, artifactsTest, projectTest, buildChangedTest) = value {
      val res = Build.prepareBuild(
        inputs,
        sourcesTest,
        generatedSourcesTest,
        options0Test,
        None,
        Scope.Test,
        bspServerProxy.currentBloopCompiler,
        persistentLogger
      )
      res.left.map((_, Scope.Test))
    }

    localClient.setGeneratedSources(Scope.Main, generatedSourcesMain)
    localClient.setGeneratedSources(Scope.Test, generatedSourcesTest)

    val mainScope = PreBuildData(
      sourcesMain,
      options0Main,
      classesDir0Main,
      scalaParamsMain,
      artifactsMain,
      projectMain,
      generatedSourcesMain,
      buildChangedMain
    )

    val testScope = PreBuildData(
      sourcesTest,
      options0Test,
      classesDir0Test,
      scalaParamsTest,
      artifactsTest,
      projectTest,
      generatedSourcesTest,
      buildChangedTest
    )

    PreBuildProject(mainScope, testScope, persistentLogger.diagnostics)
  }

  private def buildE(
    bspServerProxy: BspServerProxy,
    notifyChanges: Boolean
  ): Either[(BuildException, Scope), Unit] = {
    def doBuildOnce(data: PreBuildData, scope: Scope) =
      Build.buildOnce(
        bspServerProxy.currentBspServer.inputs,
        data.sources,
        data.generatedSources,
        data.buildOptions,
        scope,
        logger,
        actualLocalClient,
        bspServerProxy.currentBloopCompiler,
        partialOpt = None
      ).left.map(_ -> scope)

    for {
      preBuild <- prepareBuild(bspServerProxy)
      _ = {
        if (notifyChanges && (preBuild.mainScope.buildChanged || preBuild.testScope.buildChanged))
          notifyBuildChange(bspServerProxy)
      }
      _ <- doBuildOnce(preBuild.mainScope, Scope.Main)
      _ <- doBuildOnce(preBuild.testScope, Scope.Test)
    } yield ()
  }

  private def build(
    bspServerProxy: BspServerProxy,
    client: BspClient,
    notifyChanges: Boolean,
    logger: Logger
  ): Unit =
    buildE(bspServerProxy, notifyChanges) match {
      case Left((ex, scope)) =>
        client.reportBuildException(bspServerProxy.targetScopeIdOpt(scope), ex)
        logger.debug(s"Caught $ex during BSP build, ignoring it")
      case Right(()) =>
        for (targetId <- bspServerProxy.targetIds)
          client.resetBuildExceptionDiagnostics(targetId)
    }

  private val shownGlobalMessages =
    new java.util.concurrent.ConcurrentHashMap[String, Unit]()

  private def showGlobalWarningOnce(msg: String): Unit =
    shownGlobalMessages.computeIfAbsent(
      msg,
      _ => {
        val params = new b.ShowMessageParams(b.MessageType.WARNING, msg)
        actualLocalClient.onBuildShowMessage(params)
      }
    )

  def compile(
    bspServerProxy: BspServerProxy,
    executor: Executor,
    doCompile: () => CompletableFuture[b.CompileResult]
  ): CompletableFuture[b.CompileResult] = {
    val preBuild = CompletableFuture.supplyAsync(
      () =>
        prepareBuild(bspServerProxy) match {
          case Right(preBuild) =>
            if (preBuild.mainScope.buildChanged || preBuild.testScope.buildChanged)
              notifyBuildChange(bspServerProxy)
            Right(preBuild)
          case Left((ex, scope)) =>
            Left((ex, scope))
        },
      executor
    )

    preBuild.thenCompose {
      case Left((ex, scope)) =>
        actualLocalClient.reportBuildException(bspServerProxy.targetScopeIdOpt(scope), ex)
        CompletableFuture.completedFuture(
          new b.CompileResult(b.StatusCode.ERROR)
        )
      case Right(params) =>
        for (targetId <- bspServerProxy.targetIds)
          actualLocalClient.resetBuildExceptionDiagnostics(targetId)

        val targetId = bspServerProxy.targetIds.head
        params.diagnostics.foreach(actualLocalClient.reportDiagnosticForFiles(targetId))

        doCompile().thenCompose { res =>
          def doPostProcess(data: PreBuildData, scope: Scope): Unit =
            Build.postProcess(
              data.generatedSources,
              bspServerProxy.currentBspServer.inputs.generatedSrcRoot(scope),
              data.classesDir,
              logger,
              bspServerProxy.currentBspServer.inputs.workspace,
              updateSemanticDbs = true,
              scalaVersion = data.project.scalaCompiler.scalaVersion
            ).left.foreach(_.foreach(showGlobalWarningOnce))

          if (res.getStatusCode == b.StatusCode.OK)
            CompletableFuture.supplyAsync(
              () => {
                doPostProcess(params.mainScope, Scope.Main)
                doPostProcess(params.testScope, Scope.Test)
                res
              },
              executor
            )
          else
            CompletableFuture.completedFuture(res)
        }
    }
  }

  def registerWatchInputs(watcher: Build.Watcher): Unit =
    bspServerProxy.currentBspServer.inputs.elements.foreach {
      case elem: Inputs.OnDisk =>
        val eventFilter: PathWatchers.Event => Boolean = { event =>
          val newOrDeletedFile =
            event.getKind == PathWatchers.Event.Kind.Create ||
            event.getKind == PathWatchers.Event.Kind.Delete
          lazy val p        = os.Path(event.getTypedPath.getPath.toAbsolutePath)
          lazy val relPath  = p.relativeTo(elem.path)
          lazy val isHidden = relPath.segments.exists(_.startsWith("."))
          def isScalaFile   = relPath.last.endsWith(".sc") || relPath.last.endsWith(".scala")
          def isJavaFile    = relPath.last.endsWith(".java")
          newOrDeletedFile && !isHidden && (isScalaFile || isJavaFile)
        }
        val watcher0 = watcher.newWatcher()
        watcher0.register(elem.path.toNIO, Int.MaxValue)
        watcher0.addObserver {
          Build.onChangeBufferedObserver { event =>
            if (eventFilter(event))
              watcher.schedule()
          }
        }
      case _ =>
    }

  val actualLocalClient = new BspClient(
    threads.buildThreads.bloop.jsonrpc, // meh
    logger
  )
  actualLocalClient.setProjectName(initialInputs.workspace, initialInputs.projectName, Scope.Main)
  val localClient: b.BuildClient & BloopBuildClient =
    if (verbosity >= 3)
      new BspImpl.LoggingBspClient(actualLocalClient)
    else
      actualLocalClient

  var bspServerProxy: BspServerProxy = _

  val watcher = new Build.Watcher(
    ListBuffer(),
    threads.buildThreads.fileWatcher,
    build(bspServerProxy, actualLocalClient, notifyChanges = true, logger),
    ()
  )

  def run(): Future[Unit] = {
    bspServerProxy =
      new BspServerProxy(
        bloopRifleConfig = bloopRifleConfig,
        threads = threads,
        localClient = localClient,
        buildOptions = buildOptions,
        compile = doCompile =>
          compile(bspServerProxy, threads.prepareBuildExecutor, doCompile),
        logger = logger,
        initialInputs = initialInputs,
        argsToInputs = argsToInputs,
        prepareBuild = () => prepareBuild(bspServerProxy)
      )

    val localServer: b.BuildServer & b.ScalaBuildServer & b.JavaBuildServer &
      ScalaScriptBuildServer =
      if (verbosity >= 3)
        new LoggingBuildServerAll(bspServerProxy)
      else
        bspServerProxy

    val launcher = new jsonrpc.Launcher.Builder[b.BuildClient]()
      .setExecutorService(threads.buildThreads.bloop.jsonrpc) // FIXME No
      .setInput(in)
      .setOutput(out)
      .setRemoteInterface(classOf[b.BuildClient])
      .setLocalService(localServer)
      .create()
    val remoteClient = launcher.getRemoteProxy
    actualLocalClient.forwardToOpt = Some(remoteClient)
    bspServerProxy.onConnectWithClient(actualLocalClient)

    for (targetId <- bspServerProxy.currentBspServer.targetIds)
      initialInputs.flattened().foreach {
        case f: Inputs.SingleFile =>
          actualLocalClient.resetDiagnostics(f.path, targetId)
        case _: Inputs.Virtual =>
      }

    prepareBuild(bspServerProxy) match {
      case Left((ex, scope)) =>
        actualLocalClient.reportBuildException(bspServerProxy.targetScopeIdOpt(scope), ex)
        logger.log(ex)
      case Right(_) =>
    }

    logger.log {
      val hasConsole = System.console() != null
      if (hasConsole)
        "Listening to incoming JSONRPC BSP requests, press Ctrl+D to exit."
      else
        "Listening to incoming JSONRPC BSP requests."
    }
    val f = launcher.startListening()

    val initiateFirstBuild: Runnable = { () =>
      try build(bspServerProxy, actualLocalClient, notifyChanges = false, logger)
      catch {
        case t: Throwable =>
          logger.debug(s"Caught $t during initial BSP build, ignoring it")
      }
    }
    threads.prepareBuildExecutor.submit(initiateFirstBuild)

    registerWatchInputs(watcher)

    val es = ExecutionContext.fromExecutorService(threads.buildThreads.bloop.jsonrpc)
    val futures = Seq(
      BspImpl.naiveJavaFutureToScalaFuture(f).map(_ => ())(es),
      bspServerProxy.initiateShutdown
    )
    Future.firstCompletedOf(futures)(es)
  }

  def shutdown(): Unit = {
    watcher.dispose()
    if (bspServerProxy.currentBloopCompiler != null)
      bspServerProxy.currentBloopCompiler.shutdown()
  }

}

object BspImpl {

  // from https://github.com/com-lihaoyi/Ammonite/blob/7eb58c58ec8c252dc5bd1591b041fcae01cccf90/amm/interp/src/main/scala/ammonite/interp/script/AmmoniteBuildServer.scala#L550-L565
  private def naiveJavaFutureToScalaFuture[T](
    f: java.util.concurrent.Future[T]
  ): Future[T] = {
    val p = Promise[T]()
    val t = new Thread {
      setDaemon(true)
      setName("bsp-wait-for-exit")
      override def run(): Unit =
        p.complete {
          try Success(f.get())
          catch { case t: Throwable => Failure(t) }
        }
    }
    t.start()
    p.future
  }

  private final class LoggingBspClient(actualLocalClient: BspClient) extends LoggingBuildClient
      with BloopBuildClient {
    // in Scala 3 type of the method needs to be explicitly overridden
    def underlying: scala.build.bsp.BspClient = actualLocalClient
    def clear()                               = underlying.clear()
    def diagnostics                           = underlying.diagnostics
    def setProjectParams(newParams: Seq[String]) =
      underlying.setProjectParams(newParams)
    def setGeneratedSources(scope: Scope, newGeneratedSources: Seq[GeneratedSource]) =
      underlying.setGeneratedSources(scope, newGeneratedSources)
  }
}
