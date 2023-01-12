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

  test("ensure -howtorun/--how-to-run works with the default command") {
    val msg = "Hello world"
    TestInputs(os.rel / "s.sc" -> s"""println("$msg")""").fromRoot { root =>
      Seq("object", "script", "jar", "repl", "guess", "invalid").foreach { htrValue =>
        val legacyHtrOption = "-howtorun"
        val res1 =
          os.proc(TestUtil.cli, ".", legacyHtrOption, htrValue, TestUtil.extraOptions)
            .call(cwd = root, stderr = os.Pipe)
        expect(res1.out.trim() == msg)
        expect(res1.err.trim().contains(s"Deprecated option '$legacyHtrOption' is ignored"))
        expect(res1.err.trim().contains(htrValue))
        val doubleDashHtrOption = "--how-to-run"
        val res2 =
          os.proc(TestUtil.cli, ".", doubleDashHtrOption, htrValue, TestUtil.extraOptions)
            .call(cwd = root, stderr = os.Pipe)
        expect(res2.out.trim() == msg)
        expect(res2.err.trim().contains(s"Deprecated option '$doubleDashHtrOption' is ignored"))
        expect(res2.err.trim().contains(htrValue))
      }
    }
  }

  test("ensure -I works with the default command") {
    val msg = "Hello world"
    TestInputs(os.rel / "s.sc" -> s"""println("$msg")""").fromRoot { root =>
      val legacyIOption = "-I"
      val res =
        os.proc(TestUtil.cli, legacyIOption, "s.sc", "--repl-dry-run", TestUtil.extraOptions)
          .call(cwd = root, stderr = os.Pipe)
      expect(res.err.trim().contains(s"Deprecated option '$legacyIOption' is ignored"))
    }
  }
}
