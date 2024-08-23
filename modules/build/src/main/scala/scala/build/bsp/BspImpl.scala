package scala.build.bsp

import bloop.rifle.{BloopRifleConfig, BloopServer}
import ch.epfl.scala.bsp4j as b
import com.github.plokhotnyuk.jsoniter_scala.core.{JsonReaderException, readFromArray}
import dependency.ScalaParameters
import org.eclipse.lsp4j.jsonrpc
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError

import java.io.{InputStream, OutputStream}
import java.util.UUID
import java.util.concurrent.{CompletableFuture, Executor}

import scala.build.EitherCps.{either, value}
import scala.build.*
import scala.build.bsp.buildtargets.{ManagesBuildTargets, ProjectName}
import scala.build.compiler.BloopCompiler
import scala.build.errors.{
  BuildException,
  CompositeBuildException,
  Diagnostic,
  ParsingInputsException
}
import scala.build.input.{ModuleInputs, ScalaCliInvokeData, compose}
import scala.build.internal.Constants
import scala.build.options.{BuildOptions, Scope}
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success}

/** The implementation for [[Bsp]] command.
  *
  * @param argsToInputs
  *   a function transforming terminal args to [[ModuleInputs]]
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
  argsToInputs: Seq[String] => Either[BuildException, compose.Inputs],
  bspReloadableOptionsReference: BspReloadableOptions.Reference,
  threads: BspThreads,
  in: InputStream,
  out: OutputStream,
  actionableDiagnostics: Option[Boolean]
)(using ScalaCliInvokeData) extends Bsp {

  import BspImpl.{
    PreBuildData,
    PreBuildModule,
    PreBuildProject,
    buildTargetIdToEvent,
    responseError
  }

  private val shownGlobalMessages =
    new java.util.concurrent.ConcurrentHashMap[String, Unit]()
  private var actualLocalClient: BspClient                     = _
  private var localClient: b.BuildClient with BloopBuildClient = _
  private val bloopSession                                     = new BloopSession.Reference

  /** Sends the buildTarget/didChange BSP notification to the BSP client, indicating that the build
    * targets defined in the current session have changed.
    *
    * @param currentBloopSession
    *   the current Bloop session
    */
  private def notifyBuildChange(currentBloopSession: BloopSession): Unit = {
    val events =
      for (targetId <- currentBloopSession.bspServer.targetIds) yield {
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
    maybeRecoverOnError: ProjectName => BuildException => Option[BuildException] = _ => e => Some(e)
  ): Either[(BuildException, ProjectName), PreBuildProject] =
    either[(BuildException, ProjectName)] {
      val logger       = reloadableOptions.logger
      val buildOptions = reloadableOptions.buildOptions
      val verbosity    = reloadableOptions.verbosity
      logger.log("Preparing build")

      val persistentLogger = new PersistentDiagnosticLogger(logger)
      val bspServer        = currentBloopSession.bspServer

      val prebuildModules = for (module <- currentBloopSession.inputs.modules) yield {
        val mainProjectName = module.projectName
        val testProjectName = module.scopeProjectName(Scope.Test)

        // allInputs contains elements from using directives
        val (crossSources, allInputs) = value {
          CrossSources.forModuleInputs(
            inputs = module,
            preprocessors = Sources.defaultPreprocessors(
              buildOptions.archiveCache,
              buildOptions.internal.javaClassNameVersionOpt,
              () => buildOptions.javaHome().value.javaCommand
            ),
            logger = persistentLogger,
            suppressWarningOptions = buildOptions.suppressWarningOptions,
            exclude = buildOptions.internal.exclude,
            maybeRecoverOnError = maybeRecoverOnError(mainProjectName)
          ).left.map(_ -> mainProjectName)
        }

        val sharedOptions = crossSources.sharedOptions(buildOptions)

        if (verbosity >= 3)
          pprint.err.log(crossSources)

        val scopedSources =
          value(crossSources.scopedSources(buildOptions).left.map(_ -> mainProjectName))

        if (verbosity >= 3)
          pprint.err.log(scopedSources)

        val sourcesMain = value {
          scopedSources.sources(Scope.Main, sharedOptions, allInputs.workspace, persistentLogger)
            .left.map(_ -> mainProjectName)
        }

        val sourcesTest = value {
          scopedSources.sources(Scope.Test, sharedOptions, allInputs.workspace, persistentLogger)
            .left.map(_ -> testProjectName)
        }

        if (verbosity >= 3)
          pprint.err.log(sourcesMain)

        val options0Main = sourcesMain.buildOptions
        val options0Test = sourcesTest.buildOptions.orElse(options0Main)

        val generatedSourcesMain =
          sourcesMain.generateSources(allInputs.generatedSrcRoot(Scope.Main))
        val generatedSourcesTest =
          sourcesTest.generateSources(allInputs.generatedSrcRoot(Scope.Test))

        bspServer.setExtraDependencySources(options0Main.classPathOptions.extraSourceJars)
        bspServer.setExtraTestDependencySources(options0Test.classPathOptions.extraSourceJars)
        bspServer.setGeneratedSources(mainProjectName, generatedSourcesMain)
        bspServer.setGeneratedSources(testProjectName, generatedSourcesTest)

        val (classesDir0Main, scalaParamsMain, artifactsMain, projectMain, buildChangedMain) =
          value {
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
              maybeRecoverOnError(mainProjectName)
            )
            res.left.map(_ -> mainProjectName)
          }

        val (classesDir0Test, scalaParamsTest, artifactsTest, projectTest, buildChangedTest) =
          value {
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
              maybeRecoverOnError(testProjectName)
            )
            res.left.map(_ -> testProjectName)
          }

        localClient.setGeneratedSources(mainProjectName, generatedSourcesMain)
        localClient.setGeneratedSources(testProjectName, generatedSourcesTest)

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

        PreBuildModule(module, mainScope, testScope, persistentLogger.diagnostics)
      }

      PreBuildProject(prebuildModules)
    }

  private def buildE(
    currentBloopSession: BloopSession,
    notifyChanges: Boolean,
    reloadableOptions: BspReloadableOptions
  ): Either[(BuildException, ProjectName), Unit] = {
    def doBuildOnce(
      moduleInputs: ModuleInputs,
      data: PreBuildData,
      scope: Scope
    ): Either[(BuildException, ProjectName), Build] =
      Build.buildOnce(
        inputs = moduleInputs,
        sources = data.sources,
        generatedSources = data.generatedSources,
        options = data.buildOptions,
        scope = scope,
        logger = reloadableOptions.logger,
        buildClient = actualLocalClient,
        compiler = currentBloopSession.remoteServer,
        partialOpt = None
      ).left.map(_ -> moduleInputs.scopeProjectName(scope))

    either[(BuildException, ProjectName)] {
      val preBuild = value(prepareBuild(currentBloopSession, reloadableOptions))
      for (preBuildModule <- preBuild.prebuildModules) do {
        val moduleInputs = preBuildModule.inputs
        // TODO notify only specific build target
        if (
          notifyChanges && (preBuildModule.mainScope.buildChanged || preBuildModule.testScope.buildChanged)
        )
          notifyBuildChange(currentBloopSession)
        value(doBuildOnce(moduleInputs, preBuildModule.mainScope, Scope.Main))
        value(doBuildOnce(moduleInputs, preBuildModule.testScope, Scope.Test))
      }
    }
  }

  private def build(
    currentBloopSession: BloopSession,
    client: BspClient,
    notifyChanges: Boolean,
    reloadableOptions: BspReloadableOptions
  ): Unit =
    buildE(currentBloopSession, notifyChanges, reloadableOptions) match {
      case Left((ex, projectName)) =>
        client.reportBuildException(
          currentBloopSession.bspServer.targetProjectIdOpt(projectName),
          ex
        )
        reloadableOptions.logger.debug(s"Caught $ex during BSP build, ignoring it")
      case Right(()) =>
        for (targetId <- currentBloopSession.bspServer.targetIds)
          client.resetBuildExceptionDiagnostics(targetId)
    }

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
            for (preBuildModule <- preBuild.prebuildModules) do
              if (preBuildModule.mainScope.buildChanged || preBuildModule.testScope.buildChanged)
                notifyBuildChange(currentBloopSession)

            Right(preBuild)
          case Left((ex, projectName)) =>
            Left((ex, projectName))
        },
      executor
    )

    preBuild.thenCompose {
      case Left((ex, projectName)) =>
        val taskId = new b.TaskId(UUID.randomUUID().toString)

        for targetId <- currentBloopSession.bspServer.targetProjectIdOpt(projectName) do {
          val taskStartParams = new b.TaskStartParams(taskId)
          taskStartParams.setEventTime(System.currentTimeMillis())
          taskStartParams.setMessage(s"Preprocessing '$projectName'")
          taskStartParams.setDataKind(b.TaskStartDataKind.COMPILE_TASK)
          taskStartParams.setData(new b.CompileTask(targetId))

          actualLocalClient.onBuildTaskStart(taskStartParams)

          actualLocalClient.reportBuildException(
            Some(targetId),
            ex
          )

          val taskFinishParams = new b.TaskFinishParams(taskId, b.StatusCode.ERROR)
          taskFinishParams.setEventTime(System.currentTimeMillis())
          taskFinishParams.setMessage(s"Preprocessed '$projectName'")
          taskFinishParams.setDataKind(b.TaskFinishDataKind.COMPILE_REPORT)

          val errorSize = ex match {
            case c: CompositeBuildException => c.exceptions.size
            case _                          => 1
          }

          taskFinishParams.setData(new b.CompileReport(targetId, errorSize, 0))

          actualLocalClient.onBuildTaskFinish(taskFinishParams)
        }

        CompletableFuture.completedFuture(
          new b.CompileResult(b.StatusCode.ERROR)
        )
      case Right(params) =>
        for (targetId <- currentBloopSession.bspServer.targetIds)
          actualLocalClient.resetBuildExceptionDiagnostics(targetId)

        for {
          preBuildModule <- params.prebuildModules
          targetId <- currentBloopSession.bspServer
            .targetProjectIdOpt(preBuildModule.inputs.projectName)
            .toSeq
        } do
          actualLocalClient.reportDiagnosticsForFiles(
            targetId,
            preBuildModule.diagnostics,
            reset = false
          )

        doCompile().thenCompose { res =>
          def doPostProcess(inputs: ModuleInputs, data: PreBuildData, scope: Scope): Unit =
            for (sv <- data.project.scalaCompiler.map(_.scalaVersion))
              Build.postProcess(
                data.generatedSources,
                inputs.generatedSrcRoot(scope),
                data.classesDir,
                reloadableOptions.logger,
                currentBloopSession.inputs.workspace,
                updateSemanticDbs = true,
                scalaVersion = sv,
                buildOptions = data.buildOptions
              ).left.foreach(_.foreach(showGlobalWarningOnce))

          if (res.getStatusCode == b.StatusCode.OK)
            CompletableFuture.supplyAsync(
              () => {
                for (preBuildModule <- params.prebuildModules) do {
                  val moduleInputs = preBuildModule.inputs
                  doPostProcess(moduleInputs, preBuildModule.mainScope, Scope.Main)
                  doPostProcess(moduleInputs, preBuildModule.testScope, Scope.Test)
                }
                res
              },
              executor
            )
          else
            CompletableFuture.completedFuture(res)
        }
    }
  }

  /** Returns a reference to the [[BspClient]], respecting the given verbosity
    * @param verbosity
    *   verbosity to be passed to the resulting [[BspImpl.LoggingBspClient]]
    * @return
    *   BSP client
    */
  private def getLocalClient(verbosity: Int): b.BuildClient with BloopBuildClient =
    if (verbosity >= 2)
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
    inputs: compose.Inputs,
    reloadableOptions: BspReloadableOptions,
    presetIntelliJ: Boolean = false
  ): BloopSession = {
    val logger       = reloadableOptions.logger
    val buildOptions = reloadableOptions.buildOptions
    val workspace    = inputs.workspace
    val createBloopServer =
      () =>
        BloopServer.buildServer(
          reloadableOptions.bloopRifleConfig,
          "scala-cli",
          Constants.version,
          (workspace / Constants.workspaceDirName).toNIO,
          Build.classesRootDir(workspace, inputs.modules.head.projectName).toNIO,
          localClient,
          threads.buildThreads.bloop,
          logger.bloopRifleLogger
        )
    val remoteServer = new BloopCompiler(
      createBloopServer,
      20.seconds,
      strictBloopJsonCheck = buildOptions.internal.strictBloopJsonCheckOrDefault
    )
    lazy val bspServer = new BspServer(
      remoteServer.bloopServer.server,
      localClient,
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

  /** The logic for the actual running of the `bsp` command, initializing the BSP connection.
    * @param initialInputs
    *   the initial input sources passed upon initializing the BSP connection (which are subject to
    *   change on subsequent workspace/reload requests)
    */
  override def run(
    initialInputs: compose.Inputs,
    initialBspOptions: BspReloadableOptions
  ): Future[Unit] = {
    val logger    = initialBspOptions.logger
    val verbosity = initialBspOptions.verbosity

    actualLocalClient = new BspClient(logger)
    localClient = getLocalClient(verbosity)

    val currentBloopSession = newBloopSession(initialInputs, initialBspOptions)
    bloopSession.update(null, currentBloopSession, "BSP server already initialized")

    val actualLocalServer: b.BuildServer
      with b.ScalaBuildServer
      with b.JavaBuildServer
      with b.JvmBuildServer
      with ScalaScriptBuildServer
      with ManagesBuildTargets = new BuildServerProxy(
      () => bloopSession.get().bspServer,
      () => onReload()
    )

    val localServer: b.BuildServer with b.ScalaBuildServer with b.JavaBuildServer
      with b.JvmBuildServer with ScalaScriptBuildServer =
      if (verbosity >= 2)
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

    actualLocalClient.newInputs(initialInputs)
    currentBloopSession.resetDiagnostics(actualLocalClient)

    val recoverOnError: ProjectName => BuildException => Option[BuildException] = projectName =>
      e => {
        actualLocalClient.reportBuildException(actualLocalServer.targetProjectIdOpt(projectName), e)
        logger.log(e)
        None
      }

    prepareBuild(
      currentBloopSession,
      initialBspOptions,
      maybeRecoverOnError = recoverOnError
    ) match {
      case Left((ex, projectName)) => recoverOnError(projectName)(ex)
      case Right(_)                =>
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
      try build(currentBloopSession, actualLocalClient, notifyChanges = false, initialBspOptions)
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

  /** Shuts down the current Bloop session */
  override def shutdown(): Unit =
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
    previousInputs: compose.Inputs,
    newInputs: compose.Inputs,
    reloadableOptions: BspReloadableOptions
  ): CompletableFuture[AnyRef] = {
    val previousTargetIds = currentBloopSession.bspServer.targetIds
    val wasIntelliJ       = currentBloopSession.bspServer.isIntelliJ

    currentBloopSession.dispose()
    val newBloopSession0 = newBloopSession(newInputs, reloadableOptions, wasIntelliJ)
    bloopSession.update(currentBloopSession, newBloopSession0, "Concurrent reload of workspace")
    actualLocalClient.newInputs(newInputs)

    newBloopSession0.resetDiagnostics(actualLocalClient)
    prepareBuild(newBloopSession0, reloadableOptions) match {
      case Left((buildException, scope)) =>
        CompletableFuture.completedFuture(
          responseError(
            s"Can't reload workspace, build failed for scope ${scope.name}: ${buildException.message}"
          )
        )
      case Right(preBuildProject) =>
        lazy val projectJavaHome = {
          val projectBuildOptions = preBuildProject.prebuildModules
            .flatMap(m => Seq(m.mainScope.buildOptions, m.testScope.buildOptions))

          BuildOptions.pickJavaHomeWithHighestVersion(projectBuildOptions)
        }

        val finalBloopSession =
          if (
            bloopSession.get().remoteServer.jvmVersion.exists(_.value < projectJavaHome.version)
          ) {
            reloadableOptions.logger.log(
              s"Bloop JVM version too low, current ${bloopSession.get().remoteServer.jvmVersion.get
                  .value} expected ${projectJavaHome.version}, restarting server"
            )
            // ReloadableOptions don't take into account buildOptions from sources, so we need to update the bloopRifleConfig
            val updatedReloadableOptions = reloadableOptions.copy(
              bloopRifleConfig = reloadableOptions.bloopRifleConfig.copy(
                javaPath = projectJavaHome.javaCommand,
                minimumBloopJvm = projectJavaHome.version
              )
            )

            newBloopSession0.dispose()
            val bloopSessionWithJvmOkay =
              newBloopSession(newInputs, updatedReloadableOptions, wasIntelliJ)
            bloopSession.update(
              newBloopSession0,
              bloopSessionWithJvmOkay,
              "Concurrent reload of workspace"
            )
            bloopSessionWithJvmOkay
          }
          else newBloopSession0

        val previousProjectNames = previousInputs.modules.flatMap(m =>
          Seq(m.scopeProjectName(Scope.Main), m.scopeProjectName(Scope.Test))
        ).toSet
        val newProjectNames = newInputs.modules.flatMap(m =>
          Seq(m.scopeProjectName(Scope.Main), m.scopeProjectName(Scope.Test))
        ).toSet

        if (previousProjectNames != newProjectNames) {
          val client       = finalBloopSession.bspServer.bspCLient
          val newTargetIds = finalBloopSession.bspServer.targetIds
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
        val previousInputs = currentBloopSession.inputs

        val newHash      = newInputs.sourceHash
        val previousHash = currentBloopSession.inputsHash
        if newInputs == previousInputs && newHash == previousHash then
          CompletableFuture.completedFuture(new Object)
        else
          reloadBsp(currentBloopSession, previousInputs, newInputs, reloadableOptions)
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
    def setGeneratedSources(projectName: ProjectName, newGeneratedSources: Seq[GeneratedSource]) =
      underlying.setGeneratedSources(projectName, newGeneratedSources)
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

  private final case class PreBuildProject(prebuildModules: Seq[PreBuildModule])

  private final case class PreBuildModule(
    inputs: ModuleInputs,
    mainScope: PreBuildData,
    testScope: PreBuildData,
    diagnostics: Seq[Diagnostic]
  )
}
