package scala.build.internal

import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.StdIn

/** Allows for reading StdIn concurrently, in a way that it can be interrupted. It was introduced in
  * [[https://github.com/VirtusLab/scala-cli/pull/2168 #2168]] to fix input conflicts when watch and
  * interactive modes are used together. <br>
  *
  * Two scenarios are possible when a new process uses [[waitforLine]] to read StdIn:
  *   - if there is no ongoing reads taking place a future reading StdIn is started and the process
  *     waits until there's a new input line or until it is interrupted
  *   - if there is an ongoing read, the process waits for the result of the ongoing future or until
  *     it is interrupted. <br>
  *
  * __Effectively, if used in parallel, the potential input is copied and distributed among the
  * callers of [[waitForLine]]__
  */
object StdInConcurrentReader {
  private implicit val ec: ExecutionContext                           = ExecutionContext.global
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
