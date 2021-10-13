package scala.build.bsp

import ch.epfl.scala.{bsp4j => b}
import com.swoval.files.PathWatchers
import dependency.ScalaParameters
import org.eclipse.lsp4j.jsonrpc

import java.io.{InputStream, OutputStream}
import java.util.concurrent.{CompletableFuture, Executor}

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
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

  def notifyBuildChange(actualLocalServer: BspServer): Unit =
    for (targetId <- actualLocalServer.targetIdOpt) {
      val event = new b.BuildTargetEvent(targetId)
      event.setKind(b.BuildTargetEventKind.CHANGED)
      val params = new b.DidChangeBuildTarget(List(event).asJava)
      actualLocalClient.onBuildTargetDidChange(params)
    }

  private def prepareBuild(
    actualLocalServer: BspServer
  ): Either[BuildException, PreBuildData] = either {

    logger.log("Preparing build")

    val crossSources = value {
      CrossSources.forInputs(
        inputs,
        Sources.defaultPreprocessors(
          buildOptions.scriptOptions.codeWrapper.getOrElse(CustomCodeWrapper)
        )
      )
    }

    if (verbosity >= 3)
      pprint.stderr.log(crossSources)

    val scopedSources = value(crossSources.scopedSources(buildOptions))

    if (verbosity >= 3)
      pprint.stderr.log(scopedSources)

    val scope   = Scope.Main
    val sources = scopedSources.sources(scope, buildOptions)

    if (verbosity >= 3)
      pprint.stderr.log(sources)

    val options0 = sources.buildOptions

    val generatedSources = sources.generateSources(inputs.generatedSrcRoot(scope))

    actualLocalServer.setExtraDependencySources(buildOptions.classPathOptions.extraSourceJars)
    actualLocalServer.setGeneratedSources(generatedSources)

    val (classesDir0, scalaParams, artifacts, project, buildChanged) = value {
      Build.prepareBuild(
        inputs,
        sources,
        generatedSources,
        options0,
        Scope.Main,
        logger,
        localClient
      )
    }

    PreBuildData(
      sources,
      options0,
      classesDir0,
      scalaParams,
      artifacts,
      project,
      generatedSources,
      buildChanged
    )
  }

  private def buildE(
    actualLocalServer: BspServer,
    bloopServer: BloopServer,
    notifyChanges: Boolean
  ): Either[BuildException, Unit] = either {
    val preBuildData = value(prepareBuild(actualLocalServer))
    if (notifyChanges && preBuildData.buildChanged)
      notifyBuildChange(actualLocalServer)
    Build.buildOnce(
      inputs,
      preBuildData.sources,
      inputs.generatedSrcRoot(Scope.Main),
      preBuildData.generatedSources,
      preBuildData.buildOptions,
      Scope.Main,
      logger,
      actualLocalClient,
      bloopServer
    )
  }

  private def build(
    actualLocalServer: BspServer,
    bloopServer: BloopServer,
    notifyChanges: Boolean,
    logger: Logger
  ): Unit =
    buildE(actualLocalServer, bloopServer, notifyChanges) match {
      case Left(ex)  => logger.debug(s"Caught $ex during BSP build, ignoring it")
      case Right(()) =>
    }

  def compile(
    actualLocalServer: BspServer,
    executor: Executor,
    doCompile: () => CompletableFuture[b.CompileResult]
  ): CompletableFuture[b.CompileResult] = {
    val preBuild = CompletableFuture.supplyAsync(
      () => {
        val preBuildData = prepareBuild(actualLocalServer).orThrow
        if (preBuildData.buildChanged)
          notifyBuildChange(actualLocalServer)
        (
          preBuildData.classesDir,
          preBuildData.artifacts,
          preBuildData.project,
          preBuildData.generatedSources
        )
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
                inputs.generatedSrcRoot(Scope.Main),
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
    build(actualLocalServer, remoteServer, notifyChanges = true, logger),
    ()
  )

  def run(): Future[Unit] = {

    val classesDir = Build.classesDir(inputs.workspace, inputs.projectName, Scope.Main)

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

    prepareBuild(actualLocalServer) // FIXME We're discarding the error here

    logger.log {
      val hasConsole = System.console() != null
      if (hasConsole)
        "Listening to incoming JSONRPC BSP requests, press Ctrl+D to exit."
      else
        "Listening to incoming JSONRPC BSP requests."
    }
    val f = launcher.startListening()

    val initiateFirstBuild: Runnable = { () =>
      try build(actualLocalServer, remoteServer, notifyChanges = false, logger)
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
