package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class BloopTests extends munit.FunSuite {

  def runScalaCli(args: String*) = os.proc(TestUtil.cli, args)

  def testScalaTermination(
    currentBloopVersion: String,
    expectedBloopVersionAfterScalaCliRun: String
  ): Unit = TestUtil.retryOnCi() {
    def runBloop(args: String*) =
      os.proc(TestUtil.cs, "launch", s"bloop-jvm:$currentBloopVersion", "--", args)

    runBloop("exit").call()
    runBloop("about").call(stdout = os.Inherit, stderr = os.Inherit)
    runScalaCli("bloop", "start", "-v", "-v", "-v").call(
      stdout = os.Inherit,
      stderr = os.Inherit
    )
    val versionLine = runBloop("about").call().out.lines()(0)
    expect(versionLine == "bloop v" + expectedBloopVersionAfterScalaCliRun)
  }

  test("scala-cli terminates incompatible bloop") {
    testScalaTermination(Constants.oldBloopVersion, Constants.bloopVersion)
  }

  test("scala-cli keeps compatible bloop running") {
    testScalaTermination(Constants.newBloopVersion, Constants.newBloopVersion)
  }

  test("invalid bloop options passed via cli cause bloop start failure") {
    runScalaCli("bloop", "exit").call()
    val res = runScalaCli("bloop", "start", "--bloop-java-opt", "-zzefhjzl").call(
      stderr = os.Pipe,
      check = false,
      mergeErrIntoOut = true
    )
    expect(res.exitCode == 1)
    expect(res.out.text().contains("Server didn't start"))
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
      runScalaCli("bloop", "exit").call()
      val res = runScalaCli(
        "bloop",
        "start",
        "--bloop-global-options-file",
        (root / "bloop.json").toString()
      )
        .call(stderr = os.Pipe, check = false)
      expect(res.exitCode == 1)
      expect(res.err.text().contains("Server didn't start") || res.err.text().contains(
        "java.lang.OutOfMemoryError: Garbage-collected heap size exceeded"
      ))
    }
  }

}
