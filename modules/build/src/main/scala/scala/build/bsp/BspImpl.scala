package scala.build.bsp

import ch.epfl.scala.bsp4j as b
import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReaderException, readFromArray}
import dependency.ScalaParameters
import org.eclipse.lsp4j.jsonrpc
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError

import java.io.{InputStream, OutputStream}
import java.util.concurrent.{CompletableFuture, Executor}

import scala.build.EitherCps.{either, value}
import scala.build.*
import scala.build.actionable.ActionablePreprocessor
import scala.build.bloop.BloopServer
import scala.build.compiler.BloopCompiler
import scala.build.errors.{BuildException, Diagnostic, ParsingInputsException}
import scala.build.input.Inputs
import scala.build.internal.{Constants, CustomCodeWrapper}
import scala.build.options.{BuildOptions, Scope}
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success}

/** The implementation for [[Bsp]].
  *
  * @param argsToInputs
  *   a function transforming terminal args to [[Inputs]]
  * @param bspReloadableOptionsReference
  *   reference to the current instance of [[BspReloadableOptions]]
  * @param threads
  *   BSP threads
  * @param in
  *   the input stream of bytes
  * @param out
  *   the output stream of bytes
  */
final class BspImpl(
  argsToInputs: Seq[String] => Either[BuildException, Inputs],
  bspReloadableOptionsReference: BspReloadableOptions.Reference,
  threads: BspThreads,
  in: InputStream,
  out: OutputStream,
  actionableDiagnostics: Option[Boolean]
) extends Bsp {

  import BspImpl.{PreBuildData, PreBuildProject, buildTargetIdToEvent, responseError}

  /** Sends the buildTarget/didChange BSP notification to the BSP client, indicating that the build
    * targets defined in the current session have changed.
    *
    * @param currentBloopSession
    *   the current Bloop session
    */
  private def notifyBuildChange(currentBloopSession: BloopSession): Unit = {
    val events =
      for (targetId <- currentBloopSession.bspServer.targetIds)
        yield {
          val event = new b.BuildTargetEvent(targetId)
          event.setKind(b.BuildTargetEventKind.CHANGED)
          event
        }
    val params = new b.DidChangeBuildTarget(events.asJava)
    actualLocalClient.onBuildTargetDidChange(params)
  }

  /** Initial setup for the Bloop project.
    *
    * @param currentBloopSession
    *   the current Bloop session
    * @param reloadableOptions
    *   options which may be reloaded on a bsp workspace/reload request
    * @param maybeRecoverOnError
    *   a function handling [[BuildException]] instances based on [[Scope]], possibly recovering
    *   them; returns None on recovery, Some(e: BuildException) otherwise
    */
  private def prepareBuild(
    currentBloopSession: BloopSession,
    reloadableOptions: BspReloadableOptions,
    maybeRecoverOnError: Scope => BuildException => Option[BuildException] = _ => e => Some(e)
  ): Either[(BuildException, Scope), PreBuildProject] = either[(BuildException, Scope)] {
    val logger       = reloadableOptions.logger
    val buildOptions = reloadableOptions.buildOptions
    val verbosity    = reloadableOptions.verbosity
    logger.log("Preparing build")

    val persistentLogger = new PersistentDiagnosticLogger(logger)
    val bspServer        = currentBloopSession.bspServer
    val inputs           = currentBloopSession.inputs

    // allInputs contains elements from using directives
    val (crossSources, allInputs) = value {
      CrossSources.forInputs(
        inputs,
        Sources.defaultPreprocessors(
          buildOptions.scriptOptions.codeWrapper.getOrElse(CustomCodeWrapper),
          buildOptions.archiveCache,
          buildOptions.internal.javaClassNameVersionOpt,
          () => buildOptions.javaHome().value.javaCommand
        ),
        persistentLogger,
        maybeRecoverOnError(Scope.Main)
      ).left.map((_, Scope.Main))
    }

    if (verbosity >= 3)
      pprint.err.log(crossSources)

    val scopedSources = value(crossSources.scopedSources(buildOptions).left.map((_, Scope.Main)))

    if (verbosity >= 3)
      pprint.err.log(scopedSources)

    val sourcesMain = scopedSources.sources(Scope.Main, crossSources.sharedOptions(buildOptions))
    val sourcesTest = scopedSources.sources(Scope.Test, crossSources.sharedOptions(buildOptions))

    if (verbosity >= 3)
      pprint.err.log(sourcesMain)

    val options0Main = sourcesMain.buildOptions
    val options0Test = sourcesTest.buildOptions.orElse(options0Main)

    val generatedSourcesMain = sourcesMain.generateSources(allInputs.generatedSrcRoot(Scope.Main))
    val generatedSourcesTest = sourcesTest.generateSources(allInputs.generatedSrcRoot(Scope.Test))

    bspServer.setExtraDependencySources(options0Main.classPathOptions.extraSourceJars)
    bspServer.setGeneratedSources(Scope.Main, generatedSourcesMain)
    bspServer.setGeneratedSources(Scope.Test, generatedSourcesTest)

    val (classesDir0Main, scalaParamsMain, artifactsMain, projectMain, buildChangedMain) = value {
      val res = Build.prepareBuild(
        allInputs,
        sourcesMain,
        generatedSourcesMain,
        options0Main,
        None,
        Scope.Main,
        currentBloopSession.remoteServer,
        persistentLogger,
        localClient,
        maybeRecoverOnError(Scope.Main)
      )
      res.left.map((_, Scope.Main))
    }

    val (classesDir0Test, scalaParamsTest, artifactsTest, projectTest, buildChangedTest) = value {
      val res = Build.prepareBuild(
        allInputs,
        sourcesTest,
        generatedSourcesTest,
        options0Test,
        None,
        Scope.Test,
        currentBloopSession.remoteServer,
        persistentLogger,
        localClient,
        maybeRecoverOnError(Scope.Test)
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

    if (actionableDiagnostics.getOrElse(true)) {
      val projectOptions = options0Test.orElse(options0Main)
      projectOptions.logActionableDiagnostics(persistentLogger)
    }

    PreBuildProject(mainScope, testScope, persistentLogger.diagnostics)
  }

  private def buildE(
    currentBloopSession: BloopSession,
    notifyChanges: Boolean,
    reloadableOptions: BspReloadableOptions
  ): Either[(BuildException, Scope), Unit] = {
    def doBuildOnce(data: PreBuildData, scope: Scope): Either[(BuildException, Scope), Build] =
      Build.buildOnce(
        currentBloopSession.inputs,
        data.sources,
        data.generatedSources,
        data.buildOptions,
        scope,
        reloadableOptions.logger,
        actualLocalClient,
        currentBloopSession.remoteServer,
        partialOpt = None
      ).left.map(_ -> scope)

    either[(BuildException, Scope)] {
      val preBuild = value(prepareBuild(currentBloopSession, reloadableOptions))
      if (notifyChanges && (preBuild.mainScope.buildChanged || preBuild.testScope.buildChanged))
        notifyBuildChange(currentBloopSession)
      value(doBuildOnce(preBuild.mainScope, Scope.Main))
      value(doBuildOnce(preBuild.testScope, Scope.Test))
      ()
    }
  }

  private def build(
    currentBloopSession: BloopSession,
    client: BspClient,
    notifyChanges: Boolean,
    reloadableOptions: BspReloadableOptions
  ): Unit =
    buildE(currentBloopSession, notifyChanges, reloadableOptions) match {
      case Left((ex, scope)) =>
        client.reportBuildException(
          currentBloopSession.bspServer.targetScopeIdOpt(scope),
          ex
        )
        reloadableOptions.logger.debug(s"Caught $ex during BSP build, ignoring it")
      case Right(()) =>
        for (targetId <- currentBloopSession.bspServer.targetIds)
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

  /** Compilation logic, to be called on a buildTarget/compile BSP request.
    *
    * @param currentBloopSession
    *   the current Bloop session
    * @param executor
    *   executor
    * @param reloadableOptions
    *   options which may be reloaded on a bsp workspace/reload request
    * @param doCompile
    *   (self-)reference to calling the compilation logic
    * @return
    *   a future of [[b.CompileResult]]
    */
  private def compile(
    currentBloopSession: BloopSession,
    executor: Executor,
    reloadableOptions: BspReloadableOptions,
    doCompile: () => CompletableFuture[b.CompileResult]
  ): CompletableFuture[b.CompileResult] = {
    val preBuild = CompletableFuture.supplyAsync(
      () =>
        prepareBuild(currentBloopSession, reloadableOptions) match {
          case Right(preBuild) =>
            if (preBuild.mainScope.buildChanged || preBuild.testScope.buildChanged)
              notifyBuildChange(currentBloopSession)
            Right(preBuild)
          case Left((ex, scope)) =>
            Left((ex, scope))
        },
      executor
    )

    preBuild.thenCompose {
      case Left((ex, scope)) =>
        actualLocalClient.reportBuildException(
          currentBloopSession.bspServer.targetScopeIdOpt(scope),
          ex
        )
        CompletableFuture.completedFuture(
          new b.CompileResult(b.StatusCode.ERROR)
        )
      case Right(params) =>
        for (targetId <- currentBloopSession.bspServer.targetIds)
          actualLocalClient.resetBuildExceptionDiagnostics(targetId)

        val targetId = currentBloopSession.bspServer.targetIds.head
        params.diagnostics.foreach(actualLocalClient.reportDiagnosticForFiles(targetId))

        doCompile().thenCompose { res =>
          def doPostProcess(data: PreBuildData, scope: Scope): Unit =
            for (sv <- data.project.scalaCompiler.map(_.scalaVersion))
              Build.postProcess(
                data.generatedSources,
                currentBloopSession.inputs.generatedSrcRoot(scope),
                data.classesDir,
                reloadableOptions.logger,
                currentBloopSession.inputs.workspace,
                updateSemanticDbs = true,
                scalaVersion = sv
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

  private var actualLocalClient: BspClient                     = _
  private var localClient: b.BuildClient with BloopBuildClient = _

  /** Returns a reference to the [[BspClient]], respecting the given verbosity
    * @param verbosity
    *   verbosity to be passed to the resulting [[BspImpl.LoggingBspClient]]
    * @return
    *   BSP client
    */
  private def getLocalClient(verbosity: Int): b.BuildClient with BloopBuildClient =
    if (verbosity >= 3)
      new BspImpl.LoggingBspClient(actualLocalClient)
    else
      actualLocalClient

  /** Creates a fresh Bloop session
    * @param inputs
    *   all the inputs to be included in the session's context
    * @param reloadableOptions
    *   options which may be reloaded on a bsp workspace/reload request
    * @param presetIntelliJ
    *   a flag marking if this is in context of a BSP connection with IntelliJ (allowing to pass
    *   this setting from a past session)
    * @return
    *   a new [[BloopSession]]
    */
  private def newBloopSession(
    inputs: Inputs,
    reloadableOptions: BspReloadableOptions,
    presetIntelliJ: Boolean = false
  ): BloopSession = {
    val logger       = reloadableOptions.logger
    val buildOptions = reloadableOptions.buildOptions
    val bloopServer = BloopServer.buildServer(
      reloadableOptions.bloopRifleConfig,
      "scala-cli",
      Constants.version,
      (inputs.workspace / Constants.workspaceDirName).toNIO,
      Build.classesRootDir(inputs.workspace, inputs.projectName).toNIO,
      localClient,
      threads.buildThreads.bloop,
      logger.bloopRifleLogger
    )
    val remoteServer = new BloopCompiler(
      bloopServer,
      20.seconds,
      strictBloopJsonCheck = buildOptions.internal.strictBloopJsonCheckOrDefault
    )
    lazy val bspServer = new BspServer(
      remoteServer.bloopServer.server,
      doCompile =>
        compile(bloopSession0, threads.prepareBuildExecutor, reloadableOptions, doCompile),
      logger,
      presetIntelliJ
    )

    lazy val watcher = new Build.Watcher(
      ListBuffer(),
      threads.buildThreads.fileWatcher,
      build(bloopSession0, actualLocalClient, notifyChanges = true, reloadableOptions),
      ()
    )
    lazy val bloopSession0: BloopSession = BloopSession(inputs, remoteServer, bspServer, watcher)

    bloopSession0.registerWatchInputs()
    bspServer.newInputs(inputs)

    bloopSession0
  }

  private val bloopSession = new BloopSession.Reference

  /** The logic for the actual running of the `bsp` command, initializing the BSP connection.
    * @param initialInputs
    *   the initial input sources passed upon initializing the BSP connection (which are subject to
    *   change on subsequent workspace/reload requests)
    */
  def run(initialInputs: Inputs): Future[Unit] = {
    val reloadableOptions = bspReloadableOptionsReference.get
    val logger            = reloadableOptions.logger
    val verbosity         = reloadableOptions.verbosity

    actualLocalClient = new BspClient(
      threads.buildThreads.bloop.jsonrpc, // meh
      logger
    )
    localClient = getLocalClient(verbosity)

    val currentBloopSession = newBloopSession(initialInputs, reloadableOptions)
    bloopSession.update(null, currentBloopSession, "BSP server already initialized")

    val actualLocalServer
      : b.BuildServer with b.ScalaBuildServer with b.JavaBuildServer with b.JvmBuildServer
        with ScalaScriptBuildServer with HasGeneratedSources =
      new BuildServerProxy(
        () => bloopSession.get().bspServer,
        () => onReload()
      )

    val localServer: b.BuildServer with b.ScalaBuildServer with b.JavaBuildServer
      with b.JvmBuildServer with ScalaScriptBuildServer =
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
    actualLocalServer.onConnectWithClient(actualLocalClient)

    localClient.onConnectWithServer(currentBloopSession.remoteServer.bloopServer.server)
    actualLocalClient.newInputs(initialInputs)
    currentBloopSession.resetDiagnostics(actualLocalClient)

    val recoverOnError: Scope => BuildException => Option[BuildException] = scope =>
      e => {
        actualLocalClient.reportBuildException(actualLocalServer.targetScopeIdOpt(scope), e)
        logger.log(e)
        None
      }

    prepareBuild(
      currentBloopSession,
      reloadableOptions,
      maybeRecoverOnError = recoverOnError
    ) match {
      case Left((ex, scope)) => recoverOnError(scope)(ex)
      case Right(_)          =>
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
      try build(currentBloopSession, actualLocalClient, notifyChanges = false, reloadableOptions)
      catch {
        case t: Throwable =>
          logger.debug(s"Caught $t during initial BSP build, ignoring it")
      }
    }
    threads.prepareBuildExecutor.submit(initiateFirstBuild)

    val es = ExecutionContext.fromExecutorService(threads.buildThreads.bloop.jsonrpc)
    val futures = Seq(
      BspImpl.naiveJavaFutureToScalaFuture(f).map(_ => ())(es),
      currentBloopSession.bspServer.initiateShutdown
    )
    Future.firstCompletedOf(futures)(es)
  }

  /** Shuts down the current Bloop session
    */
  def shutdown(): Unit =
    for (currentBloopSession <- bloopSession.getAndNullify())
      currentBloopSession.dispose()

  /** BSP reload logic, to be used on a workspace/reload BSP request
    *
    * @param currentBloopSession
    *   the current Bloop session
    * @param previousInputs
    *   all the input sources present in the context before the reload
    * @param newInputs
    *   all the input sources to be included in the new context after the reload
    * @param reloadableOptions
    *   options which may be reloaded on a bsp workspace/reload request
    * @return
    *   a future containing a valid workspace/reload response
    */
  private def reloadBsp(
    currentBloopSession: BloopSession,
    previousInputs: Inputs,
    newInputs: Inputs,
    reloadableOptions: BspReloadableOptions
  ): CompletableFuture[AnyRef] = {
    val previousTargetIds = currentBloopSession.bspServer.targetIds
    val wasIntelliJ       = currentBloopSession.bspServer.isIntelliJ
    val newBloopSession0  = newBloopSession(newInputs, reloadableOptions, wasIntelliJ)
    bloopSession.update(currentBloopSession, newBloopSession0, "Concurrent reload of workspace")
    currentBloopSession.dispose()
    actualLocalClient.newInputs(newInputs)
    localClient.onConnectWithServer(newBloopSession0.remoteServer.bloopServer.server)

    newBloopSession0.resetDiagnostics(actualLocalClient)
    prepareBuild(newBloopSession0, reloadableOptions) match {
      case Left((buildException, scope)) =>
        CompletableFuture.completedFuture(
          responseError(
            s"Can't reload workspace, build failed for scope ${scope.name}: ${buildException.message}"
          )
        )
      case Right(preBuildProject) =>
        if (previousInputs.projectName != preBuildProject.mainScope.project.projectName)
          for (client <- newBloopSession0.bspServer.clientOpt) {
            val newTargetIds = newBloopSession0.bspServer.targetIds
            val events =
              newTargetIds.map(buildTargetIdToEvent(_, b.BuildTargetEventKind.CREATED)) ++
                previousTargetIds.map(buildTargetIdToEvent(_, b.BuildTargetEventKind.DELETED))
            val didChangeBuildTargetParams = new b.DidChangeBuildTarget(events.asJava)
            client.onBuildTargetDidChange(didChangeBuildTargetParams)
          }
        CompletableFuture.completedFuture(new Object())
    }
  }

  /** All the logic surrounding a workspace/reload (establishing the new inputs, settings and
    * refreshing all the relevant variables), including the actual BSP workspace reloading.
    *
    * @return
    *   a future containing a valid workspace/reload response
    */
  private def onReload(): CompletableFuture[AnyRef] = {
    val currentBloopSession = bloopSession.get()
    bspReloadableOptionsReference.reload()
    val reloadableOptions = bspReloadableOptionsReference.get
    val logger            = reloadableOptions.logger
    val verbosity         = reloadableOptions.verbosity
    actualLocalClient.logger = logger
    localClient = getLocalClient(verbosity)
    val ideInputsJsonPath =
      currentBloopSession.inputs.workspace / Constants.workspaceDirName / "ide-inputs.json"
    if (os.isFile(ideInputsJsonPath)) {
      val maybeResponse = either[BuildException] {
        val ideInputs = value {
          try Right(readFromArray(os.read.bytes(ideInputsJsonPath))(IdeInputs.codec))
          catch {
            case e: JsonReaderException =>
              logger.debug(s"Caught $e while decoding $ideInputsJsonPath")
              Left(new ParsingInputsException(e.getMessage, e))
          }
        }
        val newInputs      = value(argsToInputs(ideInputs.args))
        val newHash        = newInputs.sourceHash()
        val previousInputs = currentBloopSession.inputs
        val previousHash   = currentBloopSession.inputsHash
        if newInputs == previousInputs && newHash == previousHash then
          CompletableFuture.completedFuture(new Object)
        else reloadBsp(currentBloopSession, previousInputs, newInputs, reloadableOptions)
      }
      maybeResponse match {
        case Left(errorMessage) =>
          CompletableFuture.completedFuture(
            responseError(s"Workspace reload failed, couldn't load sources: $errorMessage")
          )
        case Right(r) => r
      }
    }
    else
      CompletableFuture.completedFuture(
        responseError(
          s"Workspace reload failed, inputs file missing from workspace directory: ${ideInputsJsonPath.toString()}"
        )
      )
  }
}

object BspImpl {

  private def buildTargetIdToEvent(
    targetId: b.BuildTargetIdentifier,
    eventKind: b.BuildTargetEventKind
  ): b.BuildTargetEvent = {
    val event = new b.BuildTargetEvent(targetId)
    event.setKind(eventKind)
    event
  }

  private def responseError(
    message: String,
    errorCode: Int = JsonRpcErrorCodes.InternalError
  ): ResponseError =
    new ResponseError(errorCode, message, new Object())

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

  private final case class PreBuildData(
    sources: Sources,
    buildOptions: BuildOptions,
    classesDir: os.Path,
    scalaParams: Option[ScalaParameters],
    artifacts: Artifacts,
    project: Project,
    generatedSources: Seq[GeneratedSource],
    buildChanged: Boolean
  )

  private final case class PreBuildProject(
    mainScope: PreBuildData,
    testScope: PreBuildData,
    diagnostics: Seq[Diagnostic]
  )
}
