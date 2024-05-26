package scala.build.bsp

import com.swoval.files.PathWatchers

import java.util.concurrent.atomic.AtomicReference

import scala.build.Build
import scala.build.compiler.BloopCompiler
import scala.build.input.{ModuleInputs, OnDisk, SingleFile, Virtual}

final class BloopSession(
  val inputs: Seq[ModuleInputs],
//  val inputsHash: String, TODO Fix inputs hash comparing
  val remoteServer: BloopCompiler,
  val bspServer: BspServer,
  val watcher: Build.Watcher
) {
  def resetDiagnostics(localClient: BspClient): Unit = for {
    moduleInputs <- inputs
    targetId     <- bspServer.targetProjectIdOpt(moduleInputs.projectName)
  } do
    moduleInputs.flattened().foreach {
      case f: SingleFile =>
        localClient.resetDiagnostics(f.path, targetId)
      case _: Virtual =>
    }

  def dispose(): Unit = {
    watcher.dispose()
    remoteServer.shutdown()
  }

  def registerWatchInputs(): Unit = for {
    module  <- inputs
    element <- module.elements
  } do
    element match {
      case elem: OnDisk =>
        val eventFilter: PathWatchers.Event => Boolean = { event =>
          val newOrDeletedFile =
            event.getKind == PathWatchers.Event.Kind.Create ||
            event.getKind == PathWatchers.Event.Kind.Delete
          lazy val p        = os.Path(event.getTypedPath.getPath.toAbsolutePath)
          lazy val relPath  = p.relativeTo(elem.path)
          lazy val isHidden = relPath.segments.exists(_.startsWith("."))

          def isScalaFile = relPath.last.endsWith(".sc") || relPath.last.endsWith(".scala")

          def isJavaFile = relPath.last.endsWith(".java")

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
}

object BloopSession {

  def apply(
    inputs: Seq[ModuleInputs],
    remoteServer: BloopCompiler,
    bspServer: BspServer,
    watcher: Build.Watcher
  ): BloopSession = new BloopSession(inputs, remoteServer, bspServer, watcher)

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
