package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import java.io.File
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ExecutorService, Executors, ScheduledExecutorService, ThreadFactory}
import java.util.{Locale, UUID}

import scala.Console.*
import scala.annotation.tailrec
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.jdk.CollectionConverters.*
import scala.util.{Properties, Try}

object TestUtil {

  val cliKind: String               = sys.props("test.scala-cli.kind")
  val isNativeCli: Boolean          = cliKind.startsWith("native")
  val isJvmCli: Boolean             = cliKind.startsWith("jvm")
  val isJvmBootstrappedCli: Boolean = cliKind.startsWith("jvmBootstrapped")
  val isCI: Boolean                 = System.getenv("CI") != null
  val isM1: Boolean                 = sys.props.get("os.arch").contains("aarch64")
  val cliPath: String               = sys.props("test.scala-cli.path")
  val debugPortOpt: Option[String]  = sys.props.get("test.scala-cli.debug.port")
  val detectCliPath: String         = if (TestUtil.isNativeCli) TestUtil.cliPath else "scala-cli"
  val cli: Seq[String]              = cliCommand(cliPath)
  val ltsEqualsNext: Boolean        = Constants.scala3Lts equals Constants.scala3Next

  lazy val legacyScalaVersionsOnePerMinor: Seq[String] =
    Constants.legacyScala3Versions.sorted.reverse.distinctBy(_.split('.').take(2).mkString("."))

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

  val extraOptions: List[String] = List(
    "--bloop-startup-timeout",
    "2min",
    "--bloop-bsp-timeout",
    "1min"
  )

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
      val counter                        = new AtomicInteger
      def threadNumber(): Int            = counter.incrementAndGet()
      def newThread(r: Runnable): Thread =
        new Thread(r, s"$prefix-thread-${threadNumber()}") {
          setDaemon(true)
          setPriority(Thread.NORM_PRIORITY)
        }
    }

  def normalizeUri(uri: String): String =
    if (uri.startsWith("file:///")) "file:/" + uri.stripPrefix("file:///")
    else uri

  def removeAnsiColors(str: String): String = str.replaceAll("\\e\\[[0-9]+m", "")

  def fullStableOutput(result: os.CommandResult): String =
    removeAnsiColors(result.toString).trim().linesIterator.filterNot { str =>
      // these lines are not stable and can easily change
      val shouldNotContain =
        Set(
          "Starting compilation server",
          "hint",
          "Download",
          "Result of",
          "Checking",
          "Checked",
          "Failed to download"
        )
      shouldNotContain.exists(str.contains)
    }.mkString(System.lineSeparator())

  def fullStableOutputLines(result: os.CommandResult): Vector[String] =
    fullStableOutput(result).lines().toList.asScala.toVector

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
        proc.destroy(shutdownGracePeriod = 0)
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

  // Copied from https://github.com/scalacenter/bloop/blob/a249e0a710ce169ca05d0606778f96f44a398680/shared/src/main/scala/bloop/io/Environment.scala
  private lazy val shebangCapableShells = Seq(
    "/bin/sh",
    "/bin/ash",
    "/bin/bash",
    "/bin/dash",
    "/bin/mksh",
    "/bin/pdksh",
    "/bin/posh",
    "/bin/tcsh",
    "/bin/zsh",
    "/bin/fish"
  )

  def isShebangCapableShell = Option(System.getenv("SHELL")) match {
    case Some(currentShell) if shebangCapableShells.exists(sh => currentShell.contains(sh)) => true
    case _                                                                                  => false
  }

  def readLine(
    stream: os.SubProcess.OutputStream,
    ec: ExecutionContext,
    timeout: Duration
  ): String = {
    implicit val ec0 = ec
    val readLineF    = Future {
      stream.readLine()
    }
    Await.result(readLineF, timeout)
  }

  def normalizeConsoleOutput(text: String) = {
    val allColors =
      Set(BOLD, BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE, /* GREY */ "\u001b[90m")
    allColors.+(Console.RESET).fold(text) { (textAcc, colorStr) =>
      textAcc.replace(colorStr, "")
    }
  }

  def initializeGit(
    cwd: os.Path,
    tag: String = "test-inputs",
    gitUserName: String = "testUser",
    gitUserEmail: String = "testUser@scala-cli-tests.com"
  ): Unit = {
    println(s"Initializing git in $cwd...")
    os.proc("git", "init").call(cwd = cwd)
    println(s"Setting git user.name to $gitUserName")
    os.proc("git", "config", "--local", "user.name", gitUserName).call(cwd = cwd)
    println(s"Setting git user.email to $gitUserEmail")
    os.proc("git", "config", "--local", "user.email", gitUserEmail).call(cwd = cwd)
    println(s"Adding $cwd to git...")
    os.proc("git", "add", ".").call(cwd = cwd)
    println(s"Doing an initial commit...")
    os.proc("git", "commit", "-m", "git init test inputs").call(cwd = cwd)
    println(s"Tagging as $tag...")
    os.proc("git", "tag", tag).call(cwd = cwd)
    println(s"Git initialized at $cwd")
  }

  def maybeUseBash(cmd: os.Shellable*)(cwd: os.Path = null): os.CommandResult = {
    val res = os.proc(cmd*).call(cwd = cwd, check = false)
    if (Properties.isLinux && res.exitCode == 127)
      // /bin/sh seems to have issues with '%' signs in PATH, that coursier can leave
      // in the JVM path entry (https://unix.stackexchange.com/questions/126955/percent-in-path-environment-variable)
      os.proc((("/bin/bash": os.Shellable) +: cmd)*).call(cwd = cwd)
    else {
      expect(res.exitCode == 0)
      res
    }
  }

  def withProcessWatching(
    proc: os.SubProcess,
    threadName: String = UUID.randomUUID().toString,
    poolSize: Int = 2,
    timeout: Duration = 90.seconds
  )(f: (os.SubProcess, Duration, ExecutionContext) => Unit): Unit =
    try withThreadPool(threadName, poolSize) { pool =>
        f(proc, timeout, ExecutionContext.fromExecutorService(pool))
      }
    finally if (proc.isAlive()) {
        proc.destroy()
        Thread.sleep(200L)
        if (proc.isAlive()) proc.destroy(shutdownGracePeriod = 0)
      }

  implicit class StringOps(a: String) {
    def countOccurrences(b: String): Int =
      if (b.isEmpty) 0 // Avoid infinite splitting
      else a.sliding(b.length).count(_ == b)
  }

  def printStderrUntilCondition(
    proc: os.SubProcess,
    timeout: Duration = 90.seconds
  )(condition: String => Boolean)(
    f: String => Unit = _ => ()
  )(implicit ec: ExecutionContext): Unit = {
    def revertTriggered(): Boolean = {
      val stderrOutput = TestUtil.readLine(proc.stderr, ec, timeout)
      println(stderrOutput)
      f(stderrOutput)
      condition(stderrOutput)
    }

    while (!revertTriggered()) Thread.sleep(100L)
  }

  implicit class ProcOps(proc: os.SubProcess) {
    def printStderrUntilJlineRevertsToDumbTerminal(proc: os.SubProcess)(
      f: String => Unit
    )(implicit ec: ExecutionContext): Unit =
      TestUtil.printStderrUntilCondition(proc)(_.contains("creating a dumb terminal"))(f)

    def printStderrUntilRerun(timeout: Duration)(implicit ec: ExecutionContext): Unit =
      TestUtil.printStderrUntilCondition(proc, timeout)(_.contains("re-run"))()
  }

  // based on the implementation from bloop-rifle:
  // https://github.com/scalacenter/bloop/blob/65b0b290fddd6d4256665014a7d16531e29ded4f/bloop-rifle/src/main/scala/bloop/rifle/VersionUtil.scala#L13-L30
  def parseJavaVersion(input: String): Option[Int] = {
    val jvmReleaseRegex                             = "(1[.])?(\\d+)"
    def jvmRelease(jvmVersion: String): Option[Int] = for {
      regexMatch    <- jvmReleaseRegex.r.findFirstMatchIn(jvmVersion)
      versionString <- Option(regexMatch.group(2))
      versionInt    <- Try(versionString.toInt).toOption
    } yield versionInt
    for {
      firstMatch         <- s""".*version .($jvmReleaseRegex).*""".r.findFirstMatchIn(input)
      versionNumberGroup <- Option(firstMatch.group(1))
      versionInt         <- jvmRelease(versionNumberGroup)
    } yield versionInt
  }

  extension [T](f: scala.concurrent.Future[T]) {
    def await: T                         = Await.result(f, Duration.Inf)
    def awaitWithTimeout(d: Duration): T = Await.result(f, d)
  }
}
