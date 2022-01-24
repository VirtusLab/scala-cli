package scala.build.bsp

import ch.epfl.scala.{bsp4j => b}
import com.swoval.files.PathWatchers
import dependency.ScalaParameters
import org.eclipse.lsp4j.jsonrpc

import java.io.{InputStream, OutputStream}
import java.util.concurrent.{CompletableFuture, Executor}

import scala.build.EitherCps.{either, value}
import scala.build._
import scala.build.bloop.BloopServer
import scala.build.blooprifle.BloopRifleConfig
import scala.build.errors.BuildException
import scala.build.internal.{Constants, CustomCodeWrapper}
import scala.build.options.{BuildOptions, Scope}
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success}

final class BspImpl(
  logger: Logger,
  bloopRifleConfig: BloopRifleConfig,
  inputs: Inputs,
  buildOptions: BuildOptions,
  verbosity: Int,
  threads: BspThreads,
  in: InputStream,
  out: OutputStream
) extends Bsp {

  import BspImpl.PreBuildData

  def notifyBuildChange(actualLocalServer: BspServer): Unit = {
    val events =
      for (targetId <- actualLocalServer.targetIds)
        yield {
          val event = new b.BuildTargetEvent(targetId)
          event.setKind(b.BuildTargetEventKind.CHANGED)
          event
        }
    val params = new b.DidChangeBuildTarget(events.asJava)
    actualLocalClient.onBuildTargetDidChange(params)
  }

  private def prepareBuild(
    actualLocalServer: BspServer
  ): Either[(BuildException, Scope), (PreBuildData, PreBuildData)] = either {
    logger.log("Preparing build")

    val crossSources = value {
      CrossSources.forInputs(
        inputs,
        Sources.defaultPreprocessors(
          buildOptions.scriptOptions.codeWrapper.getOrElse(CustomCodeWrapper)
        ),
        logger
      ).left.map((_, Scope.Main))
    }

    if (verbosity >= 3)
      pprint.stderr.log(crossSources)

    val scopedSources = value(crossSources.scopedSources(buildOptions))

    if (verbosity >= 3)
      pprint.stderr.log(scopedSources)

    val sourcesMain = scopedSources.sources(Scope.Main, crossSources.sharedOptions(buildOptions))
    val sourcesTest = scopedSources.sources(Scope.Test, crossSources.sharedOptions(buildOptions))

    if (verbosity >= 3)
      pprint.stderr.log(sourcesMain)

    val options0Main = sourcesMain.buildOptions
    val options0Test = sourcesTest.buildOptions.orElse(options0Main)

    val generatedSourcesMain = sourcesMain.generateSources(inputs.generatedSrcRoot(Scope.Main))
    val generatedSourcesTest = sourcesTest.generateSources(inputs.generatedSrcRoot(Scope.Test))

    actualLocalServer.setExtraDependencySources(buildOptions.classPathOptions.extraSourceJars)
    actualLocalServer.setGeneratedSources(generatedSourcesMain ++ generatedSourcesTest)

    val (classesDir0Main, scalaParamsMain, artifactsMain, projectMain, buildChangedMain) = value {
      Build.prepareBuild(
        inputs,
        sourcesMain,
        generatedSourcesMain,
        options0Main,
        Scope.Main,
        logger,
        localClient
      ) match {
        case Right(v) => Right(v)
        case Left(e)  => Left((e, Scope.Main))
      }
    }

    val (classesDir0Test, scalaParamsTest, artifactsTest, projectTest, buildChangedTest) = value {
      Build.prepareBuild(
        inputs,
        sourcesTest,
        generatedSourcesTest,
        options0Test,
        Scope.Test,
        logger,
        localClient
      ) match {
        case Right(v) => Right(v)
        case Left(e)  => Left((e, Scope.Main))
      }
    }

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

    (mainScope, testScope)
  }

  private def buildE(
    actualLocalServer: BspServer,
    bloopServer: BloopServer,
    notifyChanges: Boolean
  ): Either[(BuildException, Scope), Unit] = either {
    val (preBuildDataMain, preBuildDataTest) =
      value(prepareBuild(actualLocalServer))
    if (notifyChanges && (preBuildDataMain.buildChanged || preBuildDataTest.buildChanged))
      notifyBuildChange(actualLocalServer)
    Build.buildOnce(
      inputs,
      preBuildDataMain.sources,
      inputs.generatedSrcRoot(Scope.Main),
      preBuildDataMain.generatedSources,
      preBuildDataMain.buildOptions,
      Scope.Main,
      logger,
      actualLocalClient,
      bloopServer
    ).swap.map(e => (e, Scope.Main)).swap
    Build.buildOnce(
      inputs,
      preBuildDataTest.sources,
      inputs.generatedSrcRoot(Scope.Test),
      preBuildDataTest.generatedSources,
      preBuildDataTest.buildOptions,
      Scope.Test,
      logger,
      actualLocalClient,
      bloopServer
    ).swap.map(e => (e, Scope.Test)).swap
  }

  private def build(
    actualLocalServer: BspServer,
    bloopServer: BloopServer,
    client: BspClient,
    notifyChanges: Boolean,
    logger: Logger
  ): Unit =
    buildE(actualLocalServer, bloopServer, notifyChanges) match {
      case Left((ex, scope)) =>
        client.reportBuildException(actualLocalServer.targetScopeIdOpt(scope), ex)
        logger.debug(s"Caught $ex during BSP build, ignoring it")
      case Right(()) =>
        for (targetId <- actualLocalServer.targetIds)
          client.resetBuildExceptionDiagnostics(targetId)
    }

  private val shownGlobalMessages =
    new java.util.concurrent.ConcurrentHashMap[String, Unit]()

  private def showGlobalWarningOnce(msg: String) =
    shownGlobalMessages.computeIfAbsent(
      msg,
      _ => {
        val params = new b.ShowMessageParams(b.MessageType.WARNING, msg)
        actualLocalClient.onBuildShowMessage(params)
      }
    )

  def compile(
    actualLocalServer: BspServer,
    executor: Executor,
    doCompile: () => CompletableFuture[b.CompileResult]
  ): CompletableFuture[b.CompileResult] = {
    val preBuild = CompletableFuture.supplyAsync(
      () =>
        prepareBuild(actualLocalServer) match {
          case Right((preBuildDataMain, preBuildDataTest)) =>
            if (preBuildDataMain.buildChanged || preBuildDataTest.buildChanged)
              notifyBuildChange(actualLocalServer)
            Right((
              preBuildDataMain.classesDir,
              preBuildDataMain.project,
              preBuildDataMain.generatedSources,
              preBuildDataTest.classesDir,
              preBuildDataTest.project,
              preBuildDataTest.generatedSources
            ))
          case Left((ex, scope)) =>
            Left((ex, scope))
        },
      executor
    )

    preBuild.thenCompose { maybeParams =>
      maybeParams match {
        case Left((ex, scope)) =>
          actualLocalClient.reportBuildException(actualLocalServer.targetScopeIdOpt(scope), ex)
          CompletableFuture.completedFuture(
            new b.CompileResult(b.StatusCode.ERROR)
          )
        case Right(params) =>
          for (targetId <- actualLocalServer.targetIds)
            actualLocalClient.resetBuildExceptionDiagnostics(targetId)
          doCompile().thenCompose { res =>
            val (
              classesDir0,
              project,
              generatedSources,
              classesDir0Test,
              projectTest,
              generatedSourcesTest
            ) = params
            if (res.getStatusCode == b.StatusCode.OK)
              CompletableFuture.supplyAsync(
                () => {
                  Build.postProcess(
                    generatedSources,
                    inputs.generatedSrcRoot(Scope.Main),
                    classesDir0,
                    logger,
                    inputs.workspace,
                    updateSemanticDbs = true,
                    scalaVersion = project.scalaCompiler.scalaVersion
                  ).left.foreach(_.foreach(showGlobalWarningOnce))
                  Build.postProcess(
                    generatedSourcesTest,
                    inputs.generatedSrcRoot(Scope.Test),
                    classesDir0Test,
                    logger,
                    inputs.workspace,
                    updateSemanticDbs = true,
                    scalaVersion = projectTest.scalaCompiler.scalaVersion
                  ).left.foreach(_.foreach(showGlobalWarningOnce))
                  res
                },
                executor
              )
            else
              CompletableFuture.completedFuture(res)
          }
      }
    }
  }

  def registerWatchInputs(watcher: Build.Watcher): Unit =
    inputs.elements.foreach {
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
  actualLocalClient.setProjectName(inputs.workspace, inputs.projectName)
  val localClient: b.BuildClient with BloopBuildClient =
    if (verbosity >= 3)
      new BspImpl.LoggingBspClient(actualLocalClient)
    else
      actualLocalClient

  var remoteServer: BloopServer    = null
  var actualLocalServer: BspServer = null

  val watcher = new Build.Watcher(
    ListBuffer(),
    threads.buildThreads.fileWatcher,
    build(actualLocalServer, remoteServer, actualLocalClient, notifyChanges = true, logger),
    ()
  )

  def run(): Future[Unit] = {

    val classesDir = Build.classesRootDir(inputs.workspace, inputs.projectName)

    remoteServer = BloopServer.buildServer(
      bloopRifleConfig,
      "scala-cli",
      Constants.version,
      (inputs.workspace / ".scala").toNIO,
      classesDir.toNIO,
      localClient,
      threads.buildThreads.bloop,
      logger.bloopRifleLogger
    )
    localClient.onConnectWithServer(remoteServer.server)

    actualLocalServer =
      new BspServer(
        remoteServer.server,
        compile = doCompile =>
          compile(actualLocalServer, threads.prepareBuildExecutor, doCompile),
        logger = logger
      )
    actualLocalServer.setProjectName(inputs.workspace, inputs.projectName)
    actualLocalServer.setProjectTestName(inputs.workspace, inputs.projectName)

    val localServer: b.BuildServer with b.ScalaBuildServer with b.JavaBuildServer =
      if (verbosity >= 3)
        new LoggingBuildServerAll(actualLocalServer)
      else
        actualLocalServer

    val launcher = new jsonrpc.Launcher.Builder[b.BuildClient]()
      .setExecutorService(threads.buildThreads.bloop.jsonrpc) // FIXME No
      .setInput(in)
      .setOutput(out)
      .setRemoteInterface(classOf[b.BuildClient])
      .setLocalService(localServer)
      .create()
    val remoteClient = launcher.getRemoteProxy
    actualLocalClient.forwardToOpt = Some(remoteClient)

    for (targetId <- actualLocalServer.targetIds)
      inputs.flattened().foreach {
        case f: Inputs.SingleFile =>
          actualLocalClient.resetDiagnostics(f.path, targetId)
        case _: Inputs.Virtual =>
      }

    prepareBuild(actualLocalServer) match {
      case Left((ex, scope)) =>
        actualLocalClient.reportBuildException(actualLocalServer.targetScopeIdOpt(scope), ex)
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
      try build(actualLocalServer, remoteServer, actualLocalClient, notifyChanges = false, logger)
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
      actualLocalServer.initiateShutdown
    )
    Future.firstCompletedOf(futures)(es)
  }

  def shutdown(): Unit = {
    watcher.dispose()
    if (remoteServer != null)
      remoteServer.shutdown()
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
    def underlying  = actualLocalClient
    def clear()     = underlying.clear()
    def diagnostics = underlying.diagnostics
    def setProjectParams(newParams: Seq[String]) =
      underlying.setProjectParams(newParams)
    def setGeneratedSources(newGeneratedSources: Seq[GeneratedSource]) =
      underlying.setGeneratedSources(newGeneratedSources)
  }

  private final case class PreBuildData(
    sources: Sources,
    buildOptions: BuildOptions,
    classesDir: os.Path,
    scalaParams: ScalaParameters,
    artifacts: Artifacts,
    project: Project,
    generatedSources: Seq[GeneratedSource],
    buildChanged: Boolean
  )
}
