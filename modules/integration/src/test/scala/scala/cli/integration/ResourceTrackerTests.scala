package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.util.concurrent.ConcurrentLinkedQueue

import scala.concurrent.{Future, Promise}
import scala.jdk.CollectionConverters.*
import scala.util.Properties

class ResourceTrackerTests extends munit.FunSuite {

  private final class RecordingResourceTracker(events: ConcurrentLinkedQueue[String])
      extends ResourceTracker {
    override protected def drainSubprocess(proc: os.SubProcess): Unit =
      events.add("subprocess")

    override protected def drainThread(thread: Thread): Unit =
      events.add("thread")

    override protected def drainFuture(future: Future[?]): Unit =
      events.add("future")
  }

  private final class NoOpResourceTracker extends ResourceTracker {
    override protected def drainSubprocess(proc: os.SubProcess): Unit = ()

    override protected def drainThread(thread: Thread): Unit = ()

    override protected def drainFuture(future: Future[?]): Unit = ()
  }

  private def eventsToList(events: ConcurrentLinkedQueue[String]): List[String] =
    events.iterator().asScala.toList

  private def shortLivedSubprocess(): os.SubProcess =
    if Properties.isWin then os.proc("cmd", "/c", "exit", "0").spawn()
    else os.proc("true").spawn()

  test("drain order is subprocess -> thread -> future") {
    val events  = new ConcurrentLinkedQueue[String]
    val tracker = new RecordingResourceTracker(events)

    val future1 = Promise[Unit]().future
    val future2 = Promise[Unit]().future
    val thread  = new Thread(() => ())
    val proc1   = shortLivedSubprocess()
    val proc2   = shortLivedSubprocess()

    tracker.trackFuture(future1)
    tracker.trackSubprocess(proc1)
    tracker.trackThread(thread)
    tracker.trackFuture(future2)
    tracker.trackSubprocess(proc2)

    tracker.drain()

    expect(
      eventsToList(events) == List("subprocess", "subprocess", "thread", "future", "future")
    )
  }

  test("drain consumes tracked resources (idempotent on empty)") {
    val events  = new ConcurrentLinkedQueue[String]
    val tracker = new RecordingResourceTracker(events)

    tracker.trackFuture(Promise[Unit]().future)
    tracker.trackSubprocess(shortLivedSubprocess())
    tracker.drain()
    val afterFirstDrain = eventsToList(events)

    tracker.drain()

    expect(eventsToList(events) == afterFirstDrain)
  }

  test("clear discards without invoking drain hooks") {
    val events  = new ConcurrentLinkedQueue[String]
    val tracker = new RecordingResourceTracker(events)

    tracker.trackFuture(Promise[Unit]().future)
    tracker.trackSubprocess(shortLivedSubprocess())
    tracker.trackThread(new Thread(() => ()))
    tracker.clear()
    tracker.drain()

    expect(eventsToList(events) == Nil)
  }

  test("track* returns the tracked instance") {
    val tracker = new NoOpResourceTracker
    val thread  = new Thread(() => ())
    val future  = Promise[Unit]().future

    expect(tracker.trackThread(thread) == thread)
    expect(tracker.trackFuture(future) == future)
  }
}
