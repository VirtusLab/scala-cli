package sclicheck

import scala.annotation.tailrec
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object TestUtil {
  val isCI: Boolean = System.getenv("CI") != null

  def withTmpDir[T](prefix: String)(f: os.Path => T): T = {
    val tmpDir = os.temp.dir(prefix = prefix)
    try f(tmpDir)
    finally tryRemoveAll(tmpDir)
  }

  def tryRemoveAll(f: os.Path): Unit =
    try os.remove.all(f)
    catch {
      case ex: java.nio.file.FileSystemException =>
        System.err.println(s"Could not remove $f ($ex), ignoring it.")
    }

  lazy val scalaCliPath =
    Option(System.getenv("SCLICHECK_SCALA_CLI")).map(os.Path(_, os.pwd)).getOrElse {
      sys.error("SCLICHECK_SCALA_CLI not set")
    }

  def retry[T](
    maxAttempts: Int = 3,
    waitDuration: FiniteDuration = 5.seconds
  )(
    run: => T
  ): T = {
    @tailrec
    def helper(count: Int): T =
      try run
      catch {
        case t: Throwable =>
          if (count >= maxAttempts) {
            System.err.println(s"$maxAttempts attempts failed, caught $t. Giving up.")
            throw new Exception(t)
          }
          else {
            val remainingAttempts = maxAttempts - count
            System.err.println(
              s"Caught $t, $remainingAttempts attempts remaining, trying again in $waitDuration…"
            )
            Thread.sleep(waitDuration.toMillis)
            System.err.println(s"Trying attempt $count out of $maxAttempts...")
            helper(count + 1)
          }
      }

    helper(1)
  }

  def retryOnCi[T](maxAttempts: Int = 3, waitDuration: FiniteDuration = 5.seconds)(
    run: => T
  ): T = retry(if (isCI) maxAttempts else 1, waitDuration)(run)
}
