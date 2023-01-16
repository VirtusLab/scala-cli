package scala.cli.integration

import java.io.File
import java.net.ServerSocket
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ExecutorService, Executors, ScheduledExecutorService, ThreadFactory}

import scala.annotation.tailrec
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Properties

object TestUtil {

  val cliKind: String              = sys.props("test.scala-cli.kind")
  val isNativeCli: Boolean         = cliKind.startsWith("native")
  val isCI: Boolean                = System.getenv("CI") != null
  val cliPath: String              = sys.props("test.scala-cli.path")
  val debugPortOpt: Option[String] = sys.props.get("test.scala-cli.debug.port")
  val detectCliPath                = if (TestUtil.isNativeCli) TestUtil.cliPath else "scala-cli"
  val cli: Seq[String]             = cliCommand(cliPath)

  def cliCommand(cliPath: String): Seq[String] =
    if (isNativeCli)
      Seq(cliPath)
    else
      debugPortOpt match {
        case Some(port) => Seq(
            "java",
            "-Xmx512m",
            "-Xms128m",
            s"-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=$port,quiet=y",
            "-jar",
            cliPath
          )
        case _ => Seq("java", "-Xmx512m", "-Xms128m", "-jar", cliPath)
      }

  // format: off
  val extraOptions: List[String] = List(
    "--bloop-startup-timeout", "2min",
    "--bloop-bsp-timeout", "1min"
  )
  // format: on

  def fromPath(app: String): Option[String] = {

    val pathExt =
      if (Properties.isWin)
        Option(System.getenv("PATHEXT"))
          .toSeq
          .flatMap(_.split(File.pathSeparator).toSeq)
      else
        Seq("")
    val path = Seq(new File("").getAbsoluteFile) ++
      Option(System.getenv("PATH"))
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

  def cs: String = Constants.cs

  def threadPool(prefix: String, size: Int): ExecutorService =
    Executors.newFixedThreadPool(size, daemonThreadFactory(prefix))
  def withThreadPool[T](prefix: String, size: Int)(f: ExecutorService => T): T = {
    var pool: ExecutorService = null
    try {
      pool = threadPool(prefix, size)
      f(pool)
    }
    finally
      if (pool != null)
        pool.shutdown()
  }

  def scheduler(prefix: String): ScheduledExecutorService =
    Executors.newSingleThreadScheduledExecutor(daemonThreadFactory(prefix))

  private def daemonThreadFactory(prefix: String): ThreadFactory =
    new ThreadFactory {
      val counter             = new AtomicInteger
      def threadNumber(): Int = counter.incrementAndGet()
      def newThread(r: Runnable): Thread =
        new Thread(r, s"$prefix-thread-${threadNumber()}") {
          setDaemon(true)
          setPriority(Thread.NORM_PRIORITY)
        }
    }

  def normalizeUri(uri: String): String =
    if (uri.startsWith("file:///")) "file:/" + uri.stripPrefix("file:///")
    else uri

  def removeAnsiColors(str: String) = str.replaceAll("\\e\\[[0-9]+m", "")

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
  ): T = retry(if (isCI) maxAttempts else 1, waitDuration)(run)

  // Same as os.RelPath.toString, but for the use of File.separator instead of "/"
  def relPathStr(relPath: os.RelPath): String =
    (Seq.fill(relPath.ups)("..") ++ relPath.segments).mkString(File.separator)

  def kill(pid: Int): os.CommandResult =
    if (Properties.isWin)
      os.proc("taskkill", "/F", "/PID", pid).call()
    else
      os.proc("kill", pid).call()

  def pwd: os.Path =
    if (Properties.isWin)
      os.Path(os.pwd.toIO.getCanonicalFile)
    else
      os.pwd

  /** @return
    *   2 quotation marks (") when run for Windows, or a single one otherwise. This is necessary to
    *   escape the quotation marks passed in args for the Windows command line.
    */
  def argQuotationMark: String = if (Properties.isWin) "\"\"" else "\""

  def serveFilesInHttpServer[T](
    path: os.Path,
    user: String,
    password: String,
    realm: String
  )(f: (String, Int) => T): T = {
    val host = "127.0.0.1"
    val port = {
      val s = new ServerSocket(0)
      try s.getLocalPort()
      finally s.close()
    }
    val proc = os.proc(
      cs,
      "launch",
      "io.get-coursier:http-server_2.12:1.0.1",
      "--",
      "--user",
      user,
      "--password",
      password,
      "--realm",
      realm,
      "--directory",
      ".",
      "--host",
      host,
      "--port",
      port,
      "-v"
    )
      .spawn(cwd = path, mergeErrIntoOut = true)
    try {

      // a timeout around this would be great…
      System.err.println(s"Waiting for local HTTP server to get started on $host:$port…")
      var lineOpt = Option.empty[String]
      while (
        proc.isAlive() && {
          lineOpt = Some(proc.stdout.readLine())
          !lineOpt.exists(_.startsWith("Listening on "))
        }
      )
        for (l <- lineOpt)
          System.err.println(l)

      // Seems required, especially when using native launchers
      val waitFor = Duration(2L, "s")
      System.err.println(s"Waiting $waitFor")
      Thread.sleep(waitFor.toMillis)

      val t = new Thread("test-http-server-output") {
        setDaemon(true)
        override def run(): Unit = {
          var line = ""
          while (
            proc.isAlive() && {
              line = proc.stdout.readLine()
              line != null
            }
          )
            System.err.println(line)
        }
      }
      t.start()
      f(host, port)
    }
    finally {
      proc.destroy()
      Thread.sleep(100L)
      if (proc.isAlive()) {
        Thread.sleep(1000L)
        proc.destroyForcibly()
      }
    }
  }

  def putCsInPathViaEnv(binDir: os.Path): Map[String, String] = {

    val (pathVarName, currentPath) = sys.env
      .find(_._1.toLowerCase(Locale.ROOT) == "path")
      .getOrElse(("PATH", ""))
    if (Properties.isWin) {
      val dest = binDir / "cs.bat"
      if (!os.exists(dest)) {
        val script =
          s"""@echo off
             |"${TestUtil.cs}" %*
             |""".stripMargin
        os.write(dest, script, createFolders = true)
      }
    }
    else {
      val dest = binDir / "cs"
      if (!os.exists(dest)) {
        val script =
          s"""#!/usr/bin/env bash
             |exec "${TestUtil.cs}" "$$@"
             |""".stripMargin
        os.write(dest, script, "rwxr-xr-x", createFolders = true)
      }
    }
    Map(pathVarName -> s"$binDir${File.pathSeparator}$currentPath")
  }

  def readLine(
    stream: os.SubProcess.OutputStream,
    ec: ExecutionContext,
    timeout: Duration
  ): String = {
    implicit val ec0 = ec
    val readLineF = Future {
      stream.readLine()
    }
    Await.result(readLineF, timeout)
  }
}
