package scala

import scala.annotation.tailrec
import scala.concurrent.duration.DurationConversions.*
import scala.concurrent.duration.{DurationInt, FiniteDuration}

package object build {
  def retry[T](
    maxAttempts: Int = 3,
    waitDuration: FiniteDuration = 1.seconds
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
            Thread.sleep(waitDuration.toMillis)
            helper(count + 1)
          }
      }

    helper(1)
  }
}
