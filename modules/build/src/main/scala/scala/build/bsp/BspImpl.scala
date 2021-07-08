package scala.build.bsp

import ch.epfl.scala.{bsp4j => b}
import com.swoval.files.PathWatchers
import org.eclipse.lsp4j.jsonrpc

import java.io.{InputStream, OutputStream}
import java.util.concurrent.{CompletableFuture, Executor}

import scala.build.{BloopBuildClient, Build, GeneratedSource, Inputs, Logger, Sources}
import scala.build.bloop.{BloopServer, BuildServer}
import scala.build.blooprifle.BloopRifleConfig
import scala.build.internal.{Constants, CustomCodeWrapper}
import scala.build.options.BuildOptions
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future, Promise}
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

  def notifyBuildChange(actualLocalServer: BspServer): Unit =
    for (targetId <- actualLocalServer.targetIdOpt) {
      val event = new b.BuildTargetEvent(targetId)
      event.setKind(b.BuildTargetEventKind.CHANGED)
      val params = new b.DidChangeBuildTarget(List(event).asJava)
      actualLocalClient.onBuildTargetDidChange(params)
    }

  def prepareBuild(actualLocalServer: BspServer) = {

    logger.log("Preparing build")

    val sources = Sources.forInputs(
      inputs,
      buildOptions.scriptOptions.codeWrapper.getOrElse(CustomCodeWrapper)
    )

    if (verbosity >= 3)
      pprint.better.log(sources)

    val options0 = buildOptions.orElse(sources.buildOptions)

    val generatedSources = sources.generateSources(inputs.generatedSrcRoot)

    actualLocalServer.setGeneratedSources(generatedSources)

    val (classesDir0, artifacts, project, buildChanged) = Build.prepareBuild(
      inputs,
      sources,
      generatedSources,
      options0,
      logger,
      localClient
    )

    (sources, options0, classesDir0, artifacts, project, generatedSources, buildChanged)
  }

  def build(actualLocalServer: BspServer, bloopServer: BloopServer, notifyChanges: Boolean): Unit = {
    val (sources, buildOptions, classesDir0, artifacts, project, generatedSources, buildChanged) = prepareBuild(actualLocalServer)
    if (notifyChanges && buildChanged)
      notifyBuildChange(actualLocalServer)
    Build.buildOnce(
      inputs,
      sources,
      inputs.generatedSrcRoot,
      generatedSources,
      buildOptions,
      logger,
      actualLocalClient,
      bloopServer
    )
  }

  def compile(
    actualLocalServer: BspServer,
    executor: Executor,
    doCompile: () => CompletableFuture[b.CompileResult],
  ): CompletableFuture[b.CompileResult] = {
    val preBuild = CompletableFuture.supplyAsync(
      () => {
        val (_, _, classesDir0, artifacts, project, generatedSources, buildChanged) = prepareBuild(actualLocalServer)
        if (buildChanged)
          notifyBuildChange(actualLocalServer)
        (classesDir0, artifacts, project, generatedSources)
      },
      executor
    )

    preBuild.thenCompose { params =>
      doCompile().thenCompose { res =>
        val (classesDir0, artifacts, project, generatedSources) = params
        (classesDir0, artifacts, project, generatedSources, res)
        if (res.getStatusCode == b.StatusCode.OK)
          CompletableFuture.supplyAsync(
            () => {
              Build.postProcess(
                generatedSources,
                inputs.generatedSrcRoot,
                classesDir0,
                logger,
                inputs.workspace,
                updateSemanticDbs = true,
                updateTasty = true
              )
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
    inputs.elements.foreach {
      case dir: Inputs.Directory =>
        val eventFilter: PathWatchers.Event => Boolean = { event =>
          val newOrDeletedFile =
            event.getKind == PathWatchers.Event.Kind.Create ||
              event.getKind == PathWatchers.Event.Kind.Delete
          lazy val p = os.Path(event.getTypedPath.getPath.toAbsolutePath)
          lazy val relPath = p.relativeTo(dir.path)
          lazy val isHidden = relPath.segments.exists(_.startsWith("."))
          def isScalaFile = relPath.last.endsWith(".sc") || relPath.last.endsWith(".scala")
          def isJavaFile = relPath.last.endsWith(".java")
          def isConfFile = relPath.last == "scala.conf" || relPath.last.endsWith(".scala.conf")
          newOrDeletedFile && !isHidden && (isScalaFile || isJavaFile || isConfFile)
        }
        val watcher0 = watcher.newWatcher()
        watcher0.register(dir.path.toNIO, Int.MaxValue)
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

  var remoteServer: BloopServer = null
  var actualLocalServer: BspServer = null

  val watcher = new Build.Watcher(
    ListBuffer(),
    threads.buildThreads.fileWatcher,
    build(actualLocalServer, remoteServer, notifyChanges = true),
    ()
  )

  def run(): Future[Unit] = {

    val classesDir = Build.classesDir(inputs.workspace, inputs.projectName)

    remoteServer = BloopServer.buildServer(
      bloopRifleConfig,
      "scala-cli",
      Constants.version,
      inputs.workspace.toNIO,
      classesDir.toNIO,
      localClient,
      threads.buildThreads.bloop,
      logger.bloopRifleLogger
    )
    localClient.onConnectWithServer(remoteServer.server)

    actualLocalServer =
      new BspServer(
        remoteServer.server,
        compile = doCompile => compile(actualLocalServer, threads.prepareBuildExecutor, doCompile),
        logger = logger
      )
    actualLocalServer.setProjectName(inputs.workspace, inputs.projectName)

    val localServer: b.BuildServer with b.ScalaBuildServer =
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

    prepareBuild(actualLocalServer)

    logger.log {
      val hasConsole = System.console() != null
      if (hasConsole)
        "Listening to incoming JSONRPC BSP requests, press Ctrl+D to exit."
      else
        "Listening to incoming JSONRPC BSP requests."
    }
    val f = launcher.startListening()

    val f0 = threads.prepareBuildExecutor.submit {
      new Runnable {
        def run(): Unit =
          try build(actualLocalServer, remoteServer, notifyChanges = false)
          catch {
            case t: Throwable =>
              logger.debug(s"Caught $t during initial BSP build, ignoring it")
          }
      }
    }

    registerWatchInputs(watcher)

    val futures = Seq(
      BspImpl.naiveJavaFutureToScalaFuture(f).map(_ => ())(ExecutionContext.fromExecutorService(threads.buildThreads.bloop.jsonrpc)),
      actualLocalServer.initiateShutdown
    )
    Future.firstCompletedOf(futures)(ExecutionContext.fromExecutorService(threads.buildThreads.bloop.jsonrpc))
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

  private final class LoggingBspClient(actualLocalClient: BspClient) extends LoggingBuildClient with BloopBuildClient {
    def underlying = actualLocalClient
    def clear() = underlying.clear()
    def diagnostics = underlying.diagnostics
    def setGeneratedSources(newGeneratedSources: Seq[GeneratedSource]) =
      underlying.setGeneratedSources(newGeneratedSources)
  }
}
