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

  /** Number of threads backing the Bloop rifle server check pool.
    *
    * `bloop.rifle.internal.Operations.timeout` runs blocking Nailgun calls (notably `bloop about`)
    * on this pool and abandons them once they time out, leaving the thread blocked on socket I/O
    * (bloop rifle sets no socket read timeout). The single-threaded pool built by
    * `bloop.rifle.BloopThreads.create()` therefore lets one hung call starve every later one:
    * retries and the periodic server startup checks queue behind it and are bound to time out too.
    *
    * Kept deliberately small: enough to keep the periodic check running alongside a stuck call and
    * let a retry through, while still throttling how hard we hammer an already overloaded server.
    */
  private def bloopServerChecksThreadCount = 4
  private def bloopJsonrpcThreadCount      = 4

  def createBloopThreads(): _root_.bloop.rifle.BloopThreads = {
    val jsonrpc = Executors.newFixedThreadPool(
      bloopJsonrpcThreadCount,
      Util.daemonThreadFactory("scala-cli-bsp-jsonrpc")
    )
    val startServerChecks = Executors.newScheduledThreadPool(
      bloopServerChecksThreadCount,
      Util.daemonThreadFactory("scala-cli-bloop-rifle")
    )
    _root_.bloop.rifle.BloopThreads(jsonrpc, startServerChecks)
  }

  def create(): BuildThreads = {
    val fileWatcher = Executors.newSingleThreadScheduledExecutor(
      Util.daemonThreadFactory("scala-cli-file-watcher")
    )
    BuildThreads(createBloopThreads(), fileWatcher)
  }
}
