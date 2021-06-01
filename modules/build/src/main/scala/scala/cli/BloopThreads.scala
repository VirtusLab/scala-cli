package scala.cli

import java.util.concurrent.{Executors, ExecutorService, ScheduledExecutorService}

import scala.cli.internal.Util

final case class BloopThreads(
  jsonrpc: ExecutorService,
  startServerChecks: ScheduledExecutorService
) {
  def shutdown(): Unit = {
    jsonrpc.shutdown()
  }
}

object BloopThreads {
  def create(): BloopThreads = {
    val jsonrpc = Executors.newFixedThreadPool(4, Util.daemonThreadFactory("scala-cli-bsp-jsonrpc"))
    val startServerChecks = Executors.newSingleThreadScheduledExecutor(Util.daemonThreadFactory("scala-cli-bloopgun"))
    BloopThreads(jsonrpc, startServerChecks)
  }
}
