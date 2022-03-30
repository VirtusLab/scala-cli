package scala.build.bsp
import java.util.concurrent.CompletableFuture
import scala.build.{Inputs, Logger}
import scala.build.bloop.ScalaDebugServer
import ch.epfl.scala.{bsp4j => b}
import com.github.plokhotnyuk.jsoniter_scala.core

import scala.util.Try

class BspServerProxy(
  bloopServer: b.BuildServer & b.ScalaBuildServer & b.JavaBuildServer & ScalaDebugServer,
  compile: (() => CompletableFuture[b.CompileResult]) => CompletableFuture[b.CompileResult],
  logger: Logger,
  initialInputs: Inputs,
  argsToInputs: Seq[String] => Either[String, Inputs]
) extends BspServerWrapper {
  var currentBspServer = new BspServer(bloopServer, compile, logger, initialInputs)

  override def workspaceReload(): CompletableFuture[AnyRef] =
    super.workspaceReload().thenApply { res =>
      val ideInputsJsonPath = currentBspServer.workspace / ".scala-build" / "ide-inputs.json"
      if (os.isFile(ideInputsJsonPath)) for {
        ideInputs <- Try(core.readFromString(os.read(ideInputsJsonPath))(IdeInputs.codec))
        inputs = argsToInputs(ideInputs.args)
      } yield {
        inputs
      }
      res
    }
}
