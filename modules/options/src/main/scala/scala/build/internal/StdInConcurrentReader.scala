package scala.build.internal

import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.StdIn

object StdInConcurrentReader {
  private implicit val ec: ExecutionContext = ExecutionContext.global
  private val readLineFuture: AtomicReference[Future[Option[String]]] =
    new AtomicReference(Future.successful(None))

  /** Wait for a line to be read from StdIn
    *
    * @param atMost
    *   duration to wait before timeout
    * @return
    *   a line from StdIn wrapped in Some or None if end of stream was reached
    */
  def waitForLine(atMost: Duration = Duration.Inf): Option[String] = {
    val updatedFuture = readLineFuture.updateAndGet { f =>
      if f.isCompleted then Future(Option(StdIn.readLine())) else f
    }

    Await.result(updatedFuture, atMost)
  }
}
