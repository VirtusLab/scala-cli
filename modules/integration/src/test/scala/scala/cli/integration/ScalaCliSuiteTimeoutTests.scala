package scala.cli.integration

import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, Promise}

class ScalaCliSuiteTimeoutTests extends ScalaCliSuite {
  override def munitTimeout: Duration = 1.second

  private val attempts    = new AtomicInteger
  private val interrupted = Promise[Unit]()

  test("a hung test body times out".fail) {
    TestUtil.retry(maxAttempts = 3, waitDuration = 0.seconds) {
      attempts.incrementAndGet()
      try Thread.sleep(Long.MaxValue)
      catch
        case e: InterruptedException =>
          interrupted.success(())
          throw e
    }
  }

  test("a timed out test body gets interrupted and isn't retried") {
    Await.result(interrupted.future, 500.millis)
    // give a (wrongly) retried attempt the time to start
    Thread.sleep(100L)
    assertEquals(attempts.get(), 1)
  }
}
