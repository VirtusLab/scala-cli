package scala.cli.integration

import java.io.File

import scala.util.Properties
import java.util.concurrent.{Executors, ExecutorService, ScheduledExecutorService, ThreadFactory}
import java.util.concurrent.atomic.AtomicInteger

object TestUtil {

  val isNativeCli = System.getenv("IS_NATIVE_SCALA_CLI").contains("true")
  val isCI = System.getenv("CI") != null
  val cli = {
    val path = System.getenv("SCALA_CLI")
    if (isNativeCli)
      Seq(path)
    else
      Seq("java", "-Xmx512m", "-jar", path)
  }

  val extraOptions = List.empty[String]

  lazy val canRunJs = !isNativeCli || !Properties.isWin
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

  def threadPool(prefix: String, size: Int): ExecutorService =
    Executors.newFixedThreadPool(4, daemonThreadFactory(prefix))

  def scheduler(prefix: String): ScheduledExecutorService =
    Executors.newSingleThreadScheduledExecutor(daemonThreadFactory(prefix))

  private def daemonThreadFactory(prefix: String): ThreadFactory =
    new ThreadFactory {
      val counter = new AtomicInteger
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
}
