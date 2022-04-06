package scala.build.bsp

import java.util.concurrent.atomic.AtomicReference

import scala.build.compiler.BloopCompiler
import scala.build.{Build, Inputs}

final class BloopSession(
  val inputs: Inputs,
  val remoteServer: BloopCompiler,
  val bspServer: BspServer,
  val watcher: Build.Watcher
) {
  def resetDiagnostics(localClient: BspClient): Unit =
    for (targetId <- bspServer.targetIds)
      inputs.flattened().foreach {
        case f: Inputs.SingleFile =>
          localClient.resetDiagnostics(f.path, targetId)
        case _: Inputs.Virtual =>
      }
  def dispose(): Unit = {
    watcher.dispose()
    remoteServer.shutdown()
  }
}

object BloopSession {

  final class Reference {
    private val ref = new AtomicReference[BloopSession](null)
    def get(): BloopSession = {
      val session = ref.get()
      if (session == null)
        sys.error("BSP server not initialized yet")
      session
    }
    def getAndNullify(): Option[BloopSession] =
      Option(ref.getAndSet(null))
    def update(former: BloopSession, newer: BloopSession, ifError: String): Unit =
      if (!ref.compareAndSet(former, newer))
        sys.error(ifError)
  }
}
