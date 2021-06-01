package scala.cli

import java.util.concurrent.{Executors, ScheduledExecutorService}

import scala.cli.internal.Util

final case class BuildThreads(
  bloop: scala.cli.bloop.BloopThreads,
  fileWatcher: ScheduledExecutorService
) {
  def shutdown(): Unit = {
    bloop.shutdown()
    fileWatcher.shutdown()
  }
}

object BuildThreads {
  def create(): BuildThreads = {
    val bloop = scala.cli.bloop.BloopThreads.create()
    val fileWatcher = Executors.newSingleThreadScheduledExecutor(Util.daemonThreadFactory("scala-cli-file-watcher"))
    BuildThreads(bloop, fileWatcher)
  }
}
