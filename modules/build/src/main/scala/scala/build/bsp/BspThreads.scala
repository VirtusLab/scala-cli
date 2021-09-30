package scala.build.bsp

import java.util.concurrent.{ExecutorService, Executors}

import scala.build.BuildThreads
import scala.build.internal.Util

final case class BspThreads(
  buildThreads: BuildThreads,
  prepareBuildExecutor: ExecutorService
) {
  def shutdown(): Unit = {
    buildThreads.shutdown()
    prepareBuildExecutor.shutdown()
  }
}

object BspThreads {
  def withThreads[T](f: BspThreads => T): T = {
    var threads: BspThreads = null
    try {
      threads = create()
      f(threads)
    }
    finally if (threads != null) threads.shutdown()
  }
  def create(): BspThreads =
    BspThreads(
      BuildThreads.create(),
      Executors.newSingleThreadExecutor(
        Util.daemonThreadFactory("scala-cli-bsp-prepare-build-thread")
      )
    )
}
