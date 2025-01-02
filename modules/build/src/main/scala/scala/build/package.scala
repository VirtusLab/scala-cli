package scala

import scala.annotation.tailrec
import scala.concurrent.duration.DurationConversions.*
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.Random

package object build {
  def retry[T](
    maxAttempts: Int = 3,
    waitDuration: FiniteDuration = 1.seconds,
    variableWaitDelayInMs: Int = 500
  )(logger: Logger)(
    run: => T
  ): T = {
    @tailrec
    def helper(count: Int): T =
      try run
      catch {
        case t: Throwable =>
          if (count >= maxAttempts)
            throw new Exception(t)
          else {
            t.getStackTrace.foreach(ste => logger.debug(ste.toString))
            logger.log(s"Caught $t, trying again in $waitDuration…")
            val variableDelay       = Random.between(0, variableWaitDelayInMs + 1).milliseconds
            val currentWaitDuration = waitDuration + variableDelay
            Thread.sleep(currentWaitDuration.toMillis)
            helper(count + 1)
          }
      }

    helper(1)
  }
}
