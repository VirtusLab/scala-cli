package scala.build

import java.util.concurrent.{Executors, ScheduledExecutorService}

import scala.build.internal.Util

final case class BuildThreads(
  bloop: scala.build.bloop.BloopThreads,
  fileWatcher: ScheduledExecutorService
) {
  def shutdown(): Unit = {
    bloop.shutdown()
    fileWatcher.shutdown()
  }
}

object BuildThreads {
  def create(): BuildThreads = {
    val bloop = scala.build.bloop.BloopThreads.create()
    val fileWatcher = Executors.newSingleThreadScheduledExecutor(Util.daemonThreadFactory("scala-cli-file-watcher"))
    BuildThreads(bloop, fileWatcher)
  }
}
