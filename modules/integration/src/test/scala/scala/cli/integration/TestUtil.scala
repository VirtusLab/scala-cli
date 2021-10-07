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
  val cli = {
    val path = System.getenv("SCALA_CLI")
    if (isNativeCli)
      Seq(path)
    else
      Seq("java", "-Xmx512m", "-Xms128m", "-jar", path)
  }

  // format: off
  val extraOptions = List(
    "--bloop-startup-timeout", "2min",
    "--bloop-bsp-timeout", "1min"
  )
  // format: on

  lazy val canRunJs     = !isNativeCli || !Properties.isWin
  lazy val canRunNative = !Properties.isWin

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
      .toStream
      .headOption
      .map(_.getAbsolutePath)
  }

  lazy val cs = fromPath("cs").getOrElse {
    System.err.println("Warning: cannot find cs in PATH")
    "cs"
  }

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

  def retryOnCi[T](
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
    helper(if (isCI) 1 else maxAttempts)
  }
}
