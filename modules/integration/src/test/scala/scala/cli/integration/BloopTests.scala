package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.util.BloopUtil
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.Properties

class BloopTests extends ScalaCliSuite {

  def runScalaCli(args: String*): os.proc = os.proc(TestUtil.cli, args)

  private lazy val bloopDaemonDir =
    BloopUtil.bloopDaemonDir(runScalaCli("--power", "directories").call().out.text())

  val dummyInputs: TestInputs = TestInputs(
    os.rel / "Test.scala" ->
      """//> using scala "2.13"
        |object Test {
        |  def main(args: Array[String]): Unit =
        |    println("Hello " + "from test")
        |}
        |""".stripMargin
  )

  def testScalaTermination(
    currentBloopVersion: String,
    shouldRestart: Boolean
  ): Unit = TestUtil.retryOnCi() {
    dummyInputs.fromRoot { root =>
      BloopUtil.killBloop()

      val bloop = BloopUtil.bloop(currentBloopVersion, bloopDaemonDir)
      bloop(Seq("about")).call(cwd = root, stdout = os.Inherit)

      val output = os.proc(TestUtil.cli, "run", ".")
        .call(cwd = root, stderr = os.Pipe, mergeErrIntoOut = true)
        .out.text()
      expect(output.contains("Hello from test"))
      if (shouldRestart)
        output.contains("Shutting down unsupported Bloop")
      else
        output.contains("No need to restart Bloop")

      val versionLine = bloop(Seq("about")).call(cwd = root).out.lines()(0)
      expect(versionLine == "bloop v" + Constants.bloopVersion)
    }
  }

  // Disabled until we have at least 2 Bleep releases
  // test("scala-cli terminates incompatible bloop") {
  //   testScalaTermination("1.4.8-122-794af022", shouldRestart = true)
  // }

  test("scala-cli keeps compatible bloop running") {
    testScalaTermination(Constants.bloopVersion, shouldRestart = false)
  }

  test("invalid bloop options passed via global bloop config json file cause bloop start failure") {
    val inputs = TestInputs(
      os.rel / "bloop.json" ->
        """|{
           | "javaOptions" : ["-Xmx1k"]
           | }""".stripMargin
    )

    inputs.fromRoot { root =>
      runScalaCli("--power", "bloop", "exit").call()
      val res = runScalaCli(
        "--power",
        "bloop",
        "start",
        "--bloop-global-options-file",
        (root / "bloop.json").toString()
      ).call(cwd = root, stderr = os.Pipe, check = false)
      expect(res.exitCode == 1)
      expect(res.err.text().contains("Server failed with exit code 1") || res.err.text().contains(
        "java.lang.OutOfMemoryError: Garbage-collected heap size exceeded"
      ))
    }
  }

  test("bloop exit works") {
    def bloopRunning(): Boolean = {
      val javaProcesses = os.proc("jps", "-l").call().out.text()
      javaProcesses.contains("bloop.Bloop")
    }

    val inputs = TestInputs.empty
    inputs.fromRoot { _ =>
      BloopUtil.killBloop()
      TestUtil.retry()(assert(!bloopRunning()))

      val res = runScalaCli("--power", "bloop", "start").call(check = false)
      assert(res.exitCode == 0, clues(res.out.text()))
      assert(bloopRunning(), clues(res.out.text()))

      val resExit = runScalaCli("--power", "bloop", "exit").call(check = false)
      assert(resExit.exitCode == 0, clues(resExit.out.text()))
      assert(!bloopRunning())
    }
  }

  test("bloop projects and bloop compile works") {
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        """object Hello {
          |  def main(args: Array[String]): Unit =
          |    println("Hello")
          |}
          |""".stripMargin
    )
    inputs.fromRoot { root =>

      os.proc(TestUtil.cli, "compile", ".")
        .call(cwd = root, stdin = os.Inherit, stdout = os.Inherit)

      val projRes = os.proc(TestUtil.cli, "--power", "bloop", "projects")
        .call(cwd = root / Constants.workspaceDirName)

      val projList = projRes.out.trim().linesIterator.toVector

      expect(projList.length == 1)
      val proj = projList.head

      os.proc(TestUtil.cli, "--power", "bloop", "compile", proj)
        .call(cwd = root / Constants.workspaceDirName)

      val failRes = os.proc(TestUtil.cli, "--power", "bloop", "foo")
        .call(cwd = root / Constants.workspaceDirName, check = false, mergeErrIntoOut = true)
      val failOutput = failRes.out.text()
      expect(failRes.exitCode == 4)
      expect(failOutput.contains("Command not found: foo"))
    }
  }

  if (!Properties.isMac || !TestUtil.isNativeCli || !TestUtil.isCI)
    // TODO make this pass reliably on Mac CI
    test("Restart Bloop server while watching") {
      TestUtil.withThreadPool("bloop-restart-test", 2) { pool =>
        val timeout = Duration("90 seconds")
        val ec      = ExecutionContext.fromExecutorService(pool)

        def content(message: String) =
          s"""object Hello {
             |  def main(args: Array[String]): Unit =
             |    println("$message")
             |}
             |""".stripMargin
        val sourcePath = os.rel / "Hello.scala"
        val inputs = TestInputs(
          sourcePath -> content("Hello")
        )
        inputs.fromRoot { root =>
          val proc = os.proc(TestUtil.cli, "run", "--power", "--offline", "-w", ".")
            .spawn(cwd = root)
          val firstLine = TestUtil.readLine(proc.stdout, ec, timeout)
          expect(firstLine == "Hello")

          os.proc(TestUtil.cli, "--power", "bloop", "exit")
            .call(cwd = root)

          os.write.over(root / sourcePath, content("Foo"))
          val secondLine = TestUtil.readLine(proc.stdout, ec, timeout)
          expect(secondLine == "Foo")

          proc.destroy()
        }
      }
    }
}
