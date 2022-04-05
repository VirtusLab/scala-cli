package scala.build.bsp
import ch.epfl.scala.bsp4j as b
import com.github.plokhotnyuk.jsoniter_scala.core

import java.util.concurrent.CompletableFuture

import scala.build.bloop.BloopServer
import scala.build.blooprifle.BloopRifleConfig
import scala.build.compiler.BloopCompiler
import scala.build.errors.BuildException
import scala.build.internal.Constants
import scala.build.options.{BuildOptions, Scope}
import scala.build.{BloopBuildClient, Build, Inputs, Logger}
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.*
import scala.util.Try

import org.eclipse.lsp4j.jsonrpc.messages.ResponseError

class BspServerProxy(
  bloopRifleConfig: BloopRifleConfig,
  threads: BspThreads,
  localClient: b.BuildClient & BloopBuildClient,
  buildOptions: BuildOptions,
  compile: (() => CompletableFuture[b.CompileResult]) => CompletableFuture[b.CompileResult],
  logger: Logger,
  initialInputs: Inputs,
  argsToInputs: Seq[String] => Either[String, Inputs],
  prepareBuild: () => Either[(BuildException, Scope), PreBuildProject]
) extends BspServerWrapper {
  var currentBloopCompiler: BloopCompiler = createBloopCompiler(initialInputs)
  localClient.onConnectWithServer(currentBloopCompiler.bloopServer.server)
  var currentBspServer: BspServer = createBspServer(currentBloopCompiler, initialInputs)

  override def workspaceReload(): CompletableFuture[AnyRef] =
    super.workspaceReload().thenCompose { res =>
      val ideInputsJsonPath =
        currentBspServer.workspace / Constants.workspaceDirName / "ide-inputs.json"
      if (os.isFile(ideInputsJsonPath))
        (for {
          ideInputs <-
            Try(core.readFromString(os.read(ideInputsJsonPath))(IdeInputs.codec)).toEither.fold(
              t => Left(t.getMessage),
              Right(_)
            )
          newInputs <- argsToInputs(ideInputs.args)
          previousInputs = currentBspServer.inputs
        } yield
          if (newInputs != previousInputs) reloadBsp(previousInputs, newInputs)
          else CompletableFuture.completedFuture(res)) match {
          case Left(errorMessage) =>
            CompletableFuture.completedFuture(
              responseError(s"Workspace reload failed, couldn't load sources: $errorMessage")
            )
          case Right(r) => r
        }
      else CompletableFuture.completedFuture(
        responseError(s"Workspace reload failed, inputs file missing from workspace directory: ${ideInputsJsonPath.toString()}")
      )
    }

  private def reloadBsp(
    previousInputs: Inputs,
    newInputs: Inputs
  ): CompletableFuture[AnyRef] = {
    val previousTargetIds = currentBspServer.targetIds
    currentBloopCompiler = createBloopCompiler(newInputs)
    currentBspServer = createBspServer(currentBloopCompiler, newInputs)
    val newTargetIds = currentBspServer.targetIds
    prepareBuild() match {
      case Left((buildException, scope)) =>
        CompletableFuture.completedFuture(
          new ResponseError(
            JsonRpcErrorCodes.InternalError,
            s"Can't reload workspace, build failed for scope: ${scope.name}: ${buildException.message}",
            new Object()
          )
        )
      case Right(preBuildProject) =>
        if (previousInputs.projectName != preBuildProject.mainScope.project.projectName) {
          val events = newTargetIds.map(buildTargetIdToEvent(_, b.BuildTargetEventKind.CREATED)) ++
            previousTargetIds.map(buildTargetIdToEvent(_, b.BuildTargetEventKind.DELETED))
          val didChangeBuildTargetParams = new b.DidChangeBuildTarget(events.asJava)
          currentBspServer.client.foreach(_.onBuildTargetDidChange(didChangeBuildTargetParams))
        }
        CompletableFuture.completedFuture(new Object())
    }
  }

  private def createBloopCompiler(inputs: Inputs): BloopCompiler = {
    val bloopServer = BloopServer.buildServer(
      bloopRifleConfig,
      "scala-cli",
      Constants.version,
      (inputs.workspace / Constants.workspaceDirName).toNIO,
      Build.classesRootDir(inputs.workspace, inputs.projectName).toNIO,
      localClient,
      threads.buildThreads.bloop,
      logger.bloopRifleLogger
    )
    new BloopCompiler(
      bloopServer,
      20.seconds,
      strictBloopJsonCheck = buildOptions.internal.strictBloopJsonCheckOrDefault
    )
  }

  private def createBspServer(bloopCompiler: BloopCompiler, inputs: Inputs) =
    new BspServer(bloopCompiler.bloopServer.server, compile, logger, inputs)

  private def buildTargetIdToEvent(
    targetId: b.BuildTargetIdentifier,
    eventKind: b.BuildTargetEventKind
  ): b.BuildTargetEvent = {
    val event = new b.BuildTargetEvent(targetId)
    event.setKind(eventKind)
    event
  }

  private def responseError(message: String, errorCode: Int = JsonRpcErrorCodes.InternalError): ResponseError =
    new ResponseError(errorCode, message, new Object())
}
