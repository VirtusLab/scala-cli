package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class DeprecationTests extends ScalaCliSuite {
  override def group: ScalaCliSuite.TestGroup = ScalaCliSuite.TestGroup.First

  private val configFile = os.rel / "config" / "config.json"
  private val configEnvs = Map("SCALA_CLI_CONFIG" -> configFile.toString())

  test("deprecated CLI option warning includes exact option name and detail") {
    val inputPath = os.rel / "example.sc"
    TestInputs(inputPath -> """println("hello")""").fromRoot { root =>
      val res = os.proc(
        TestUtil.cli,
        "run",
        TestUtil.extraOptions,
        inputPath,
        "--deprecated-test-option"
      ).call(cwd = root, stderr = os.Pipe)
      val err = res.err.trim()
      expect(err.contains("--deprecated-test-option"))
      expect(err.contains("is deprecated."))
      expect(err.contains("For testing purposes only."))
    }
  }

  test("deprecated CLI option alias warning includes exact alias name") {
    val inputPath = os.rel / "example.sc"
    TestInputs(inputPath -> """println("hello")""").fromRoot { root =>
      val res = os.proc(
        TestUtil.cli,
        "run",
        TestUtil.extraOptions,
        inputPath,
        "--deprecated-test-alias"
      ).call(cwd = root, stderr = os.Pipe)
      val err = res.err.trim()
      expect(err.contains("--deprecated-test-alias"))
      expect(err.contains("is deprecated."))
    }
  }

  test("deprecated using directive produces a warning") {
    val inputPath = os.rel / "example.sc"
    TestInputs(inputPath ->
      """//> using lib "com.lihaoyi::os-lib:0.11.4"
        |println("hello")
        |""".stripMargin).fromRoot { root =>
      val res = os.proc(TestUtil.cli, "run", TestUtil.extraOptions, inputPath)
        .call(cwd = root, stderr = os.Pipe)
      val err = res.err.trim()
      expect(err.contains("deprecated"))
      expect(err.contains("lib"))
    }
  }

  test("--suppress-deprecated-warnings silences deprecation warnings") {
    val inputPath = os.rel / "example.sc"
    TestInputs(inputPath -> """println("hello")""").fromRoot { root =>
      val res = os.proc(
        TestUtil.cli,
        "run",
        TestUtil.extraOptions,
        inputPath,
        "--deprecated-test-option",
        "--suppress-deprecated-warnings"
      ).call(cwd = root, stderr = os.Pipe)
      val err = res.err.trim()
      expect(!err.contains("is deprecated"))
    }
  }

  test("config suppress-warning.deprecated-features silences deprecation warnings") {
    val inputPath = os.rel / "example.sc"
    TestInputs(inputPath -> """println("hello")""").fromRoot { root =>
      os.proc(TestUtil.cli, "config", "suppress-warning.deprecated-features", "true")
        .call(cwd = root, env = configEnvs)
      val res = os.proc(
        TestUtil.cli,
        "run",
        TestUtil.extraOptions,
        inputPath,
        "--deprecated-test-option"
      ).call(cwd = root, stderr = os.Pipe, env = configEnvs)
      val err = res.err.trim()
      expect(!err.contains("is deprecated"))
    }
  }

  test("--ammonite deprecation warning includes the exact alias used") {
    TestInputs.empty.fromRoot { root =>
      val res = os.proc(
        TestUtil.cli,
        "--power",
        "repl",
        TestUtil.extraOptions,
        "--amm",
        "--repl-dry-run"
      ).call(cwd = root, stderr = os.Pipe)
      val err = res.err.trim()
      expect(err.contains("--amm"))
      expect(err.contains("is deprecated."))
      expect(err.contains("Use the default Scala REPL instead."))
    }
  }

  test("multiple deprecated features produce a single consolidated warning") {
    val inputPath = os.rel / "example.sc"
    TestInputs(inputPath -> """println("hello")""").fromRoot { root =>
      val res = os.proc(
        TestUtil.cli,
        "run",
        TestUtil.extraOptions,
        inputPath,
        "--deprecated-test-option",
        "--deprecated-test-alias"
      ).call(cwd = root, stderr = os.Pipe)
      val err = res.err.trim()
      expect(err.contains("--deprecated-test-option"))
      expect(err.contains("--deprecated-test-alias"))
      val deprecatedWarningLines = err.linesIterator
        .count(_.contains("Deprecated features may be removed"))
      expect(deprecatedWarningLines == 1)
    }
  }
}
