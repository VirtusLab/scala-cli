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

  test("default to the run sub-command when a script snippet is passed with -e") {
    TestInputs.empty.fromRoot { root =>
      val msg       = "Hello world"
      val quotation = TestUtil.argQuotationMark
      val res =
        os.proc(TestUtil.cli, "-e", s"println($quotation$msg$quotation)", TestUtil.extraOptions)
          .call(cwd = root)
      expect(res.out.text().trim == msg)
    }
  }

  test("default to the run sub-command when a scala snippet is passed with --execute-scala") {
    TestInputs.empty.fromRoot { root =>
      val msg       = "Hello world"
      val quotation = TestUtil.argQuotationMark
      val res =
        os.proc(
          TestUtil.cli,
          "--execute-scala",
          s"@main def main() = println($quotation$msg$quotation)",
          TestUtil.extraOptions
        )
          .call(cwd = root)
      expect(res.out.text().trim == msg)
    }
  }

  test("default to the run sub-command when a java snippet is passed with --execute-java") {
    TestInputs.empty.fromRoot { root =>
      val msg       = "Hello world"
      val quotation = TestUtil.argQuotationMark
      val res =
        os.proc(
          TestUtil.cli,
          "--execute-java",
          s"public class Main { public static void main(String[] args) { System.out.println($quotation$msg$quotation); } }",
          TestUtil.extraOptions
        )
          .call(cwd = root)
      expect(res.out.text().trim == msg)
    }
  }

  test("running scala-cli with a script snippet passed with -e shouldn't allow repl-only options") {
    TestInputs.empty.fromRoot { root =>
      val replSpecificOption = "--repl-dry-run"
      val res =
        os.proc(
          TestUtil.cli,
          "-e",
          "println()",
          replSpecificOption,
          TestUtil.extraOptions
        )
          .call(cwd = root, mergeErrIntoOut = true, check = false)
      expect(res.exitCode == 1)
      expect(res.out.trim == unrecognizedArgMessage(replSpecificOption))
    }
  }

  private def unrecognizedArgMessage(argName: String) =
    s"""
       |Unrecognized argument: $argName
       |
       |To list all available options, run
       |  ${Console.BOLD}${TestUtil.detectCliPath} --help${Console.RESET}
       |""".stripMargin.trim
}
