package scala.cli.integration

import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ExecutorService, Executors, ScheduledExecutorService, ThreadFactory}

import scala.annotation.tailrec
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.Properties

object TestUtil {

  val cliKind     = System.getenv("SCALA_CLI_KIND")
  val isNativeCli = cliKind.startsWith("native")
  val isCI        = System.getenv("CI") != null
  val cliPath     = System.getenv("SCALA_CLI")
  val cli         = cliCommand(cliPath)

  def cliCommand(cliPath: String): Seq[String] =
    if (isNativeCli)
      Seq(cliPath)
    else
      Seq("java", "-Xmx512m", "-Xms128m", "-jar", cliPath)

  // format: off
  val extraOptions = List(
    "--bloop-startup-timeout", "2min",
    "--bloop-bsp-timeout", "1min"
  )
  // format: on

  lazy val canRunJs = !isNativeCli || !Properties.isWin

  def fromPath(app: String): Option[String] = {

    val pathExt =
      if (Properties.isWin)
        Option(System.getenv("PATHEXT"))
          .toSeq
          .flatMap(_.split(File.pathSeparator).toSeq)
      else
        Seq("")
    val path = Option(System.getenv("PATH"))
      .toSeq
      .flatMap(_.split(File.pathSeparator))
      .map(new File(_))

    def candidates =
      for {
        dir <- path.iterator
        ext <- pathExt.iterator
      } yield new File(dir, app + ext)

    candidates
      .filter(_.canExecute)
      .take(1)
      .toList
      .headOption
      .map(_.getAbsolutePath)
  }

  def cs = Constants.cs

  def threadPool(prefix: String, size: Int): ExecutorService =
    Executors.newFixedThreadPool(size, daemonThreadFactory(prefix))

  def scheduler(prefix: String): ScheduledExecutorService =
    Executors.newSingleThreadScheduledExecutor(daemonThreadFactory(prefix))

  private def daemonThreadFactory(prefix: String): ThreadFactory =
    new ThreadFactory {
      val counter        = new AtomicInteger
      def threadNumber() = counter.incrementAndGet()
      def newThread(r: Runnable) =
        new Thread(r, s"$prefix-thread-${threadNumber()}") {
          setDaemon(true)
          setPriority(Thread.NORM_PRIORITY)
        }
    }

  def normalizeUri(uri: String): String =
    if (uri.startsWith("file:///")) "file:/" + uri.stripPrefix("file:///")
    else uri

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
          if (count >= maxAttempts)
            throw new Exception(t)
          else {
            System.err.println(s"Caught $t, trying again in $waitDuration…")
            Thread.sleep(waitDuration.toMillis)
            helper(count + 1)
          }
      }
    helper(1)
  }

  def retryOnCi[T](maxAttempts: Int = 3, waitDuration: FiniteDuration = 5.seconds)(
    run: => T
  ) = retry((if (isCI) maxAttempts else 1), waitDuration)(run)

  // Same as os.RelPath.toString, but for the use of File.separator instead of "/"
  def relPathStr(relPath: os.RelPath): String =
    (Seq.fill(relPath.ups)("..") ++ relPath.segments).mkString(File.separator)

  def kill(pid: Int) =
    if (Properties.isWin)
      os.proc("taskkill", "/F", "/PID", pid).call()
    else
      os.proc("kill", pid).call()

}
