package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class DefaultTests extends WithWarmUpScalaCliSuite {
  override def warmUpExtraTestOptions: Seq[String] = TestUtil.extraOptions

  test("running scala-cli with no args should default to repl") {
    TestInputs.empty.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "--repl-dry-run").call(cwd = root, mergeErrIntoOut = true)
      expect(res.out.lines().lastOption.contains(replDryRunOutput))
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
      expect(res.out.lines().endsWith(unrecognizedArgMessage(runSpecificOption)))
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
      expect(res.out.lines().endsWith(unrecognizedArgMessage(replSpecificOption)))
    }
  }

  test("default to the run sub-command when a script snippet is passed with -e") {
    TestInputs.empty.fromRoot { root =>
      val msg       = "Hello world"
      val quotation = TestUtil.argQuotationMark
      val res =
        os.proc(TestUtil.cli, "-e", s"println($quotation$msg$quotation)", TestUtil.extraOptions)
          .call(cwd = root)
      expect(res.out.trim() == msg)
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
      expect(res.out.trim() == msg)
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
      expect(res.out.trim() == msg)
    }
  }

  test("default to the run sub-command when a md snippet is passed with --execute-markdown") {
    TestInputs.empty.fromRoot { root =>
      val msg       = "Hello world"
      val quotation = TestUtil.argQuotationMark
      val res =
        os.proc(
          TestUtil.cli,
          "--execute-markdown",
          s"""# A Markdown snippet
             |With some scala code
             |```scala
             |println($quotation$msg$quotation)
             |```""".stripMargin,
          TestUtil.extraOptions
        )
          .call(cwd = root)
      expect(res.out.trim() == msg)
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
      expect(res.out.lines().endsWith(unrecognizedArgMessage(replSpecificOption)))
    }
  }

  test("default to the shebang sub-command if -classpath and --main-class are passed") {
    val expectedOutput = "Hello"
    val mainClassName  = "Main"
    TestInputs(
      os.rel / s"$mainClassName.scala" -> s"""object $mainClassName extends App { println("$expectedOutput") }"""
    ).fromRoot { (root: os.Path) =>
      val compilationOutputDir = os.rel / "compilationOutput"
      // first, precompile to an explicitly specified output directory with -d
      os.proc(
        TestUtil.cli,
        ".",
        "-d",
        compilationOutputDir
      ).call(cwd = root)

      // next, run shebang while relying on the pre-compiled class instead of passing inputs
      val shebangRes = os.proc(
        TestUtil.cli,
        "--main-class",
        mainClassName,
        "-classpath",
        (os.rel / compilationOutputDir).toString
      ).call(cwd = root)
      expect(shebangRes.out.trim() == expectedOutput)
    }
  }

  test("default to the repl sub-command if -classpath is passed, but --main-class isn't") {
    val expectedOutput = "Hello"
    val mainClassName  = "Main"
    TestInputs(
      os.rel / s"$mainClassName.scala" -> s"""object $mainClassName extends App { println("$expectedOutput") }"""
    ).fromRoot { (root: os.Path) =>
      val compilationOutputDir = os.rel / "compilationOutput"
      // first, precompile to an explicitly specified output directory with -d
      os.proc(
        TestUtil.cli,
        ".",
        "-d",
        compilationOutputDir
      ).call(cwd = root)

      // next, run the repl while relying on the pre-compiled classes
      val runRes = os.proc(
        TestUtil.cli,
        "--repl-dry-run",
        "-classpath",
        (os.rel / compilationOutputDir).toString
      ).call(cwd = root, mergeErrIntoOut = true)
      expect(runRes.out.lines().lastOption.contains(replDryRunOutput))
    }
  }

  test("ensure --help-native works with the default command") {
    TestInputs.empty.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "--help-native").call(cwd = root)
      expect(res.out.trim().contains("Scala Native options:"))
    }
  }

  test("ensure --help-js works with the default command") {
    TestInputs.empty.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "--help-js").call(cwd = root)
      expect(res.out.trim().contains("Scala.js options:"))
    }
  }

  test("ensure -save/--save works with the default command") {
    val msg = "Hello world"
    TestInputs(os.rel / "s.sc" -> s"""println("$msg")""").fromRoot { root =>
      val legacySaveOption = "-save"
      val res1 =
        os.proc(TestUtil.cli, ".", legacySaveOption, TestUtil.extraOptions)
          .call(cwd = root, stderr = os.Pipe)
      expect(res1.out.trim() == msg)
      expect(res1.err.trim().contains(s"Deprecated option '$legacySaveOption' is ignored"))
      val doubleDashSaveOption = "--save"
      val res2 =
        os.proc(TestUtil.cli, ".", doubleDashSaveOption, TestUtil.extraOptions)
          .call(cwd = root, stderr = os.Pipe)
      expect(res2.out.trim() == msg)
      expect(res2.err.trim().contains(s"Deprecated option '$doubleDashSaveOption' is ignored"))
    }
  }

  test("ensure -nosave/--nosave works with the default command") {
    val msg = "Hello world"
    TestInputs(os.rel / "s.sc" -> s"""println("$msg")""").fromRoot { root =>
      val legacyNoSaveOption = "-nosave"
      val res1 =
        os.proc(TestUtil.cli, ".", legacyNoSaveOption, TestUtil.extraOptions)
          .call(cwd = root, stderr = os.Pipe)
      expect(res1.out.trim() == msg)
      expect(res1.err.trim().contains(s"Deprecated option '$legacyNoSaveOption' is ignored"))
      val doubleDashNoSaveOption = "--nosave"
      val res2 =
        os.proc(TestUtil.cli, ".", doubleDashNoSaveOption, TestUtil.extraOptions)
          .call(cwd = root, stderr = os.Pipe)
      expect(res2.out.trim() == msg)
      expect(res2.err.trim().contains(s"Deprecated option '$doubleDashNoSaveOption' is ignored"))
    }
  }

  test("ensure natural arguments handling is supported by default") {
    TestInputs(os.rel / "s.sc" -> s"""println(args.mkString)""").fromRoot { root =>
      val msg = "Hello"
      val res = os.proc(TestUtil.cli, ".", msg, TestUtil.extraOptions)
        .call(cwd = root, stderr = os.Pipe)
      expect(res.out.trim() == msg)
    }
  }

  private def unrecognizedArgMessage(argName: String) =
    s"""
       |Unrecognized argument: $argName
       |
       |To list all available options, run
       |  ${Console.BOLD}${TestUtil.detectCliPath} --help${Console.RESET}
       |""".stripMargin.trim.linesIterator.toVector

  private lazy val replDryRunOutput = "Dry run, not running REPL."
}
