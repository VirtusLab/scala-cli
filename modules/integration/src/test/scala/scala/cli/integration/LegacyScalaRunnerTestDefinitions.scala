package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect
trait LegacyScalaRunnerTestDefinitions { _: DefaultTests =>
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

  test("ensure -save/--save works with the default command") {
    simpleLegacyOptionBackwardsCompatTest("-save", "--save")
  }

  test("ensure -nosave/--nosave works with the default command") {
    simpleLegacyOptionBackwardsCompatTest("-nosave", "--nosave")
  }

  test("ensure -howtorun/--how-to-run works with the default command") {
    legacyOptionBackwardsCompatTest("-howtorun", "--how-to-run") {
      (legacyHtrOption, inputFile, root) =>
        Seq("object", "script", "jar", "repl", "guess", "invalid").foreach { htrValue =>
          val res =
            os.proc(TestUtil.cli, legacyHtrOption, htrValue, inputFile, TestUtil.extraOptions)
              .call(cwd = root, stderr = os.Pipe)
          expect(res.err.trim().contains(deprecatedOptionWarning(legacyHtrOption)))
          expect(res.err.trim().contains(htrValue))
        }
    }
  }

  test("ensure -I works with the default command") {
    legacyOptionBackwardsCompatTest("-I") {
      (legacyOption, inputFile, root) =>
        val anotherInputFile = "smth.scala"
        val res = os.proc(
          TestUtil.cli,
          legacyOption,
          inputFile,
          legacyOption,
          anotherInputFile,
          "--repl-dry-run",
          TestUtil.extraOptions
        )
          .call(cwd = root, stderr = os.Pipe)
        expect(res.err.trim().contains(deprecatedOptionWarning(legacyOption)))
        expect(res.err.trim().contains(inputFile))
        expect(res.err.trim().contains(anotherInputFile))
    }
  }

  test("ensure -nc/-nocompdaemon/--no-compilation-daemon works with the default command") {
    simpleLegacyOptionBackwardsCompatTest("-nc", "-nocompdaemon", "--no-compilation-daemon")
  }

  test("ensure -run works with the default command") {
    legacyOptionBackwardsCompatTest("-run") {
      (legacyOption, inputFile, root) =>
        val res = os.proc(TestUtil.cli, legacyOption, inputFile, ".", TestUtil.extraOptions)
          .call(cwd = root, stderr = os.Pipe)
        expect(res.err.trim().contains(deprecatedOptionWarning(legacyOption)))
        expect(res.err.trim().contains(inputFile))
    }
  }

  private def simpleLegacyOptionBackwardsCompatTest(optionAliases: String*): Unit =
    abstractLegacyOptionBackwardsCompatTest(optionAliases) {
      (legacyOption, expectedMsg, _, root) =>
        val res = os.proc(TestUtil.cli, legacyOption, "s.sc", TestUtil.extraOptions)
          .call(cwd = root, stderr = os.Pipe)
        expect(res.out.trim() == expectedMsg)
        expect(res.err.trim().contains(deprecatedOptionWarning(legacyOption)))
    }

  private def legacyOptionBackwardsCompatTest(optionAliases: String*)(f: (
    String,
    String,
    os.Path
  ) => Unit): Unit =
    abstractLegacyOptionBackwardsCompatTest(optionAliases) { (legacyOption, _, inputFile, root) =>
      f(legacyOption, inputFile, root)
    }

  private def abstractLegacyOptionBackwardsCompatTest(optionAliases: Seq[String])(f: (
    String,
    String,
    String,
    os.Path
  ) => Unit): Unit = {
    val msg       = "Hello world"
    val inputFile = "s.sc"
    TestInputs(os.rel / inputFile -> s"""println("$msg")""").fromRoot { root =>
      optionAliases.foreach(f(_, msg, inputFile, root))
    }
  }

  private def deprecatedOptionWarning(optionName: String) =
    s"Deprecated option '$optionName' is ignored"
}
