package scala.build

import java.util.concurrent.{Executors, ScheduledExecutorService}

import scala.build.internal.Util

final case class BuildThreads(
  bloop: _root_.bloop.rifle.BloopThreads,
  fileWatcher: ScheduledExecutorService
) {
  def shutdown(): Unit = {
    bloop.shutdown()
    fileWatcher.shutdown()
  }
}

object BuildThreads {
  def create(): BuildThreads = {
    val bloop = _root_.bloop.rifle.BloopThreads.create()
    val fileWatcher = Executors.newSingleThreadScheduledExecutor(
      Util.daemonThreadFactory("scala-cli-file-watcher")
    )
    BuildThreads(bloop, fileWatcher)
  }
}
