package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

class RunTestsDefault extends RunTestDefinitions
    with RunWithWatchTestDefinitions
    with TestDefault {
  def archLinuxTest(): Unit = {
    val message = "Hello from Scala CLI on Arch Linux"
    val inputs = TestInputs(
      os.rel / "hello.sc" ->
        s"""println("$message")
           |""".stripMargin
    )
    val extraOptsStr = extraOptions.mkString(" ") /* meh escaping */
    inputs.fromRoot { root =>
      os.copy(os.Path(TestUtil.cli.head, os.pwd), root / "scala")
      val script =
        s"""#!/usr/bin/env sh
           |set -e
           |./scala --server=false $extraOptsStr . | tee -a output
           |""".stripMargin
      os.write(root / "script.sh", script)
      os.perms.set(root / "script.sh", "rwxr-xr-x")
      val termOpt = if (System.console() == null) Nil else Seq("-t")
      val cmd = Seq[os.Shellable](
        "docker",
        "run",
        "--rm",
        termOpt,
        "-e",
        "SCALA_CLI_VENDORED_ZIS=true",
        "-v",
        s"$root:/data",
        "-w",
        "/data",
        ciOpt,
        Constants.dockerArchLinuxImage,
        "/data/script.sh"
      )
      val res = os.proc(cmd).call(cwd = root)
      System.err.println(res.out.text())
      val output = os.read(root / "output").trim
      expect(output == message)
    }
  }

  if (Properties.isLinux && TestUtil.isNativeCli)
    test("arch linux") {
      archLinuxTest()
    }

  test("3.nightly") { // should run code using scala 3 nightly version
    TestInputs(os.rel / "sample.sc" -> """println("Hello World")""").fromRoot {
      root =>
        val res =
          os.proc(
            TestUtil.cli,
            "run",
            ".",
            "-S",
            "3.nightly",
            TestUtil.extraOptions
          )
            .call(cwd = root)
        expect(res.out.trim() == "Hello World")
    }
  }

  test("as jar") {
    val inputs = TestInputs(
      os.rel / "CheckCp.scala" ->
        """//> using dep com.lihaoyi::os-lib:0.9.1
          |object CheckCp {
          |  def main(args: Array[String]): Unit = {
          |    val cp = sys.props("java.class.path")
          |      .split(java.io.File.pathSeparator)
          |      .toVector
          |      .map(os.Path(_, os.pwd))
          |    assert(cp.forall(os.isFile(_)), "Not only files")
          |  }
          |}
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "run", extraOptions, ".")
        .call(cwd = root, mergeErrIntoOut = true, check = false)
      expect(res.exitCode != 0)
      val output = res.out.text()
      expect(output.contains("java.lang.AssertionError: assertion failed: Not only files"))

      os.proc(TestUtil.cli, "--power", "run", extraOptions, ".", "--as-jar")
        .call(cwd = root)
    }
  }

  test("meaningful commas dont have to be escaped in using directive values") {
    val inputPath = os.rel / "example.scala"
    TestInputs(inputPath ->
      """//> using dep tabby:tabby:0.2.3,url=https://github.com/bjornregnell/tabby/releases/download/v0.2.3/tabby_3-0.2.3.jar
        |import tabby.Grid
        |@main def main = println(Grid("a", "b", "c")(1, 2, 3))
        |""".stripMargin).fromRoot {
      root =>
        val res = os.proc(TestUtil.cli, "run", extraOptions, inputPath)
          .call(cwd = root)
      val out = res.out.trim()
      expect(out.contains("a, b, c"))
    }
  }

  for {
    suppressDeprecatedWarnings <- Seq(true, false)
    suppressByConfig           <- if (suppressDeprecatedWarnings) Seq(true, false) else Seq(false)
    suppressWarningOptions =
      if (suppressDeprecatedWarnings && !suppressByConfig) Seq("--suppress-deprecated-warnings")
      else Seq.empty
    suppressedWarningText =
      suppressDeprecatedWarnings -> suppressByConfig match {
        case (true, true)  => "suppressed (by config)"
        case (true, false) => "suppressed (by command line option)"
        case (false, _)    => "reported"
      }
  }
    test(
      s"a deprecation warning should be $suppressedWarningText when using commas with space as separators in using directives"
    ) {
      val configFile = os.rel / "config" / "config.json"
      val configEnvs = Map("SCALA_CLI_CONFIG" -> configFile.toString())
      val inputPath  = os.rel / "example.sc"
      TestInputs(inputPath ->
        """//> using options -Werror, -Wconf:cat=deprecation:e, -Wconf:cat=unused:e
          |println("Deprecation warnings should have been printed")
          |""".stripMargin)
        .fromRoot { root =>
          if (suppressByConfig)
            os.proc(TestUtil.cli, "config", "suppress-warning.deprecated-features", "true")
              .call(cwd = root, env = configEnvs)
          val res = os.proc(TestUtil.cli, "run", extraOptions, inputPath, suppressWarningOptions)
            .call(cwd = root, stderr = os.Pipe, env = configEnvs)
          val err             = res.err.trim()
          val expectedWarning = "Use of commas as separators is deprecated"
          if (suppressDeprecatedWarnings) expect(!err.contains("deprecated"))
          else {
            expect(err.contains("deprecated"))
            expect(err.linesIterator.count(_.contains(expectedWarning)) == 2)
          }
        }
    }

  for {
    useTestScope <- Seq(true, false)
    fileName        = if (useTestScope) "example.test.scala" else "example.scala"
    scopeOptions    = if (useTestScope) Seq("--test") else Seq.empty
    scopeDesc       = if (useTestScope) "test" else "main"
    expectedMessage = "Hello, World!"
    platformOptions <- Seq(Seq("--native"), Seq("--js"), Nil)
    platformDesc = platformOptions.headOption.getOrElse("JVM").stripPrefix("--")
  }
    test(
      s"run --cross $platformDesc $actualScalaVersion, ${Constants.scala213} and ${Constants.scala212} ($scopeDesc scope)"
    ) {
      TestUtil.retryOnCi() {
        TestInputs {
          os.rel / fileName ->
            s"""//> using scala $actualScalaVersion ${Constants.scala213} ${Constants.scala212}
               |object Main extends App {
               |  println("$expectedMessage")
               |}
               |""".stripMargin
        }.fromRoot { root =>
          val r =
            os.proc(
              TestUtil.cli,
              "run",
              ".",
              "--cross",
              "--power",
              extraOptions,
              scopeOptions,
              platformOptions
            )
              .call(cwd = root)
          expect(r.out.trim() == expectedMessage)
        }
      }
    }

  for {
    scalaVersion <- TestUtil.legacyScalaVersionsOnePerMinor
    expectedMessage = "Hello, world!"
    expectedWarning =
      s"Defaulting to a legacy runner module version: ${Constants.runnerLegacyVersion}"
  }
    test(
      s"run a simple hello world with the runner module on the classpath and Scala $scalaVersion (legacy)"
    ) {
      TestInputs(os.rel / "script.sc" -> s"""println("$expectedMessage")""").fromRoot { root =>
        val res =
          os.proc(TestUtil.cli, "run", ".", "-S", scalaVersion, TestUtil.extraOptions, "--runner")
            .call(cwd = root, stderr = os.Pipe)
        expect(res.out.trim() == expectedMessage)
        expect(res.err.trim().contains(expectedWarning))
      }
    }
}
