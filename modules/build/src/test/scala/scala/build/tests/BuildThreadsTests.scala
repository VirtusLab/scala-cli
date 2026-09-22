package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

import java.util.concurrent.{CountDownLatch, TimeUnit}

import scala.build.BuildThreads

class BuildThreadsTests extends TestUtil.ScalaCliBuildSuite {

  // Bloop rifle abandons timed out Nailgun calls (i.e. `bloop about`) on this pool, leaving the
  // thread blocked. A single-threaded pool would make every subsequent call, including retries and
  // the periodic server startup checks, queue behind the stuck one and time out as well.
  test("the Bloop server check pool keeps running tasks while an earlier one is stuck") {
    val threads = BuildThreads.create()
    val pool    = threads.bloop.startServerChecks
    val release = new CountDownLatch(1)
    val started = new CountDownLatch(1)
    try {
      pool.execute { () =>
        started.countDown()
        release.await()
      }
      expect(started.await(10, TimeUnit.SECONDS))

      val unblocked = new CountDownLatch(1)
      pool.execute(() => unblocked.countDown())
      expect(unblocked.await(10, TimeUnit.SECONDS))
    }
    finally {
      release.countDown()
      threads.shutdown()
      pool.shutdown()
    }
  }
}
