package scala.cli.integration

import java.util.concurrent.{ConcurrentLinkedQueue, TimeUnit}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

private[integration] class ResourceTracker {
  private val subprocesses                   = new ConcurrentLinkedQueue[os.SubProcess]
  private val threads                        = new ConcurrentLinkedQueue[Thread]
  private val futures                        = new ConcurrentLinkedQueue[Future[?]]
  protected val drainTimeout: FiniteDuration = FiniteDuration(5, TimeUnit.SECONDS)
  private val threadJoinTimeoutMillis        = drainTimeout.toMillis

  def trackSubprocess[P <: os.SubProcess](proc: P): P = {
    subprocesses.add(proc)
    proc
  }

  def trackThread[T <: Thread](thread: T): T = {
    threads.add(thread)
    thread
  }

  def trackFuture[F <: Future[?]](future: F): F = {
    futures.add(future)
    future
  }

  def clear(): Unit = {
    subprocesses.clear()
    threads.clear()
    futures.clear()
  }

  def drain(): Unit = {
    drainAll(subprocesses, drainSubprocess)
    drainAll(threads, drainThread)
    drainAll(futures, drainFuture)
  }

  private def drainAll[A](queue: ConcurrentLinkedQueue[A], drain: A => Unit): Unit = {
    var next = queue.poll()
    while next != null do
      drain(next)
      next = queue.poll()
  }

  protected def drainSubprocess(proc: os.SubProcess): Unit =
    try
      if proc.isAlive() then
        proc.destroy()
        proc.join(drainTimeout.toMillis)
        if proc.isAlive() then
          proc.destroy(shutdownGracePeriod = 0)
          proc.join(drainTimeout.toMillis)
    catch case NonFatal(_) => ()

  protected def drainThread(thread: Thread): Unit =
    try thread.join(threadJoinTimeoutMillis)
    catch case NonFatal(_) => ()

  protected def drainFuture(future: Future[?]): Unit =
    try Await.ready(future, drainTimeout)
    catch case NonFatal(_) => ()
}
