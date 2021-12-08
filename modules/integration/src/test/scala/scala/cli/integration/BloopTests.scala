package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

class BloopTests extends munit.FunSuite {

  def runScalaCli(args: String*) = os.proc(TestUtil.cli, args)

  private lazy val bloopDaemonDir = {
    val res    = runScalaCli("directories").call()
    val output = res.out.text()
    val dir = output
      .linesIterator
      .map(_.trim)
      .filter(_.startsWith("Bloop daemon directory: "))
      .map(_.stripPrefix("Bloop daemon directory: "))
      .map(os.Path(_, os.pwd))
      .take(1)
      .toList
      .headOption
      .getOrElse {
        sys.error(s"Cannot get Bloop daemon directory in 'scala-cli directories' output '$output'")
      }
    if (!os.exists(dir)) {
      os.makeDir.all(dir)
      if (!Properties.isWin)
        os.perms.set(dir, "rwx------")
    }
    dir
  }

  private lazy val daemonArgs = {
    val dirArgs = Seq("--daemon-dir", bloopDaemonDir.toString)
    if (Properties.isWin)
      // FIXME Get the pipe name via 'scala-cli directories' too?
      dirArgs ++ Seq("--pipe-name", "scalacli\\bloop\\pipe")
    else
      dirArgs
  }

  // temporary, bleep exit does exit, but is having issues later on…
  private def exitCheck = false

  val dummyInputs = TestInputs(
    Seq(
      os.rel / "Test.scala" ->
        """// using scala "2.13"
          |object Test {
          |  def main(args: Array[String]): Unit =
          |    println("Hello " + "from test")
          |}
          |""".stripMargin
    )
  )

  def testScalaTermination(
    currentBloopVersion: String,
    shouldRestart: Boolean
  ): Unit = TestUtil.retryOnCi() {
    dummyInputs.fromRoot { root =>
      val bloopOrg =
        currentBloopVersion.split("[-.]") match {
          case Array(majStr, minStr, patchStr, _*) =>
            import scala.math.Ordering.Implicits._
            val maj   = majStr.toInt
            val min   = minStr.toInt
            val patch = patchStr.toInt
            val useBloopMainLine =
              Seq(maj, min, patch) < Seq(1, 4, 11) ||
              (Seq(maj, min, patch) == Seq(1, 4, 11) && !currentBloopVersion.endsWith("-SNAPSHOT"))
            if (useBloopMainLine)
              "ch.epfl.scala"
            else
              "io.github.alexarchambault.bleep"
          case _ =>
            "ch.epfl.scala"
        }
      def bloop(args: String*): os.proc =
        os.proc(
          TestUtil.cs,
          "launch",
          s"$bloopOrg:bloopgun_2.12:$currentBloopVersion",
          "--",
          daemonArgs,
          args
        )

      bloop("exit").call(cwd = root, stdout = os.Inherit, check = exitCheck)
      bloop("about").call(cwd = root, stdout = os.Inherit)

      val output = os.proc(TestUtil.cli, "run", ".")
        .call(cwd = root, stderr = os.Pipe, mergeErrIntoOut = true)
        .out.text()
      expect(output.contains("Hello from test"))
      if (shouldRestart)
        output.contains("Shutting down unsupported Bloop")
      else
        output.contains("No need to restart Bloop")

      val versionLine = bloop("about").call(cwd = root).out.lines()(0)
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

  test("invalid bloop options passed via cli cause bloop start failure") {
    TestInputs(Seq()).fromRoot { root =>
      runScalaCli("bloop", "exit").call(cwd = root, check = exitCheck)
      val res = runScalaCli("bloop", "start", "--bloop-java-opt", "-zzefhjzl").call(
        cwd = root,
        stderr = os.Pipe,
        check = false,
        mergeErrIntoOut = true
      )
      expect(res.exitCode == 1)
      expect(res.out.text().contains("Server didn't start"))
    }
  }

  test("invalid bloop options passed via global bloop config json file cause bloop start failure") {
    val inputs = TestInputs(
      Seq(
        os.rel / "bloop.json" ->
          """|{
             | "javaOptions" : ["-Xmx1k"]
             | }""".stripMargin
      )
    )

    inputs.fromRoot { root =>
      runScalaCli("bloop", "exit").call(check = exitCheck)
      val res = runScalaCli(
        "bloop",
        "start",
        "--bloop-global-options-file",
        (root / "bloop.json").toString()
      ).call(cwd = root, stderr = os.Pipe, check = false)
      expect(res.exitCode == 1)
      expect(res.err.text().contains("Server didn't start") || res.err.text().contains(
        "java.lang.OutOfMemoryError: Garbage-collected heap size exceeded"
      ))
    }
  }

}
