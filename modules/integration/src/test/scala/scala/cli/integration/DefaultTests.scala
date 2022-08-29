package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class DefaultTests extends ScalaCliSuite {
  test("running scala-cli with no args should default to repl") {
    TestInputs.empty.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "--repl-dry-run").call(cwd = root, mergeErrIntoOut = true)
      expect(res.out.trim() == "Dry run, not running REPL.")
    }
  }
  test("running scala-cli with no args should not accept run-only options") {
    TestInputs.empty.fromRoot { root =>
      val runSpecificOption = "--list-main-classes"
      val res = os.proc(TestUtil.cli, runSpecificOption).call(
        cwd = root,
        mergeErrIntoOut = true,
        check = false
      )
      expect(res.exitCode == 1)
      expect(res.out.trim == unrecognizedArgMessage(runSpecificOption))
    }
  }
  test("running scala-cli with args should not accept repl-only options") {
    TestInputs(os.rel / "Hello.sc" -> """println("Hello")""").fromRoot { root =>
      val replSpecificOption = "--ammonite"
      val res = os.proc(TestUtil.cli, ".", replSpecificOption).call(
        cwd = root,
        mergeErrIntoOut = true,
        check = false
      )
      expect(res.exitCode == 1)
      expect(res.out.trim == unrecognizedArgMessage(replSpecificOption))
    }
  }

  private def unrecognizedArgMessage(argName: String) = {
    val scalaCli = if (TestUtil.isNativeCli) TestUtil.cliPath else "scala-cli"
    s"""
       |Unrecognized argument: $argName
       |
       |To list all available options, run
       |  ${Console.BOLD}$scalaCli --help${Console.RESET}
       |""".stripMargin.trim
  }
}
