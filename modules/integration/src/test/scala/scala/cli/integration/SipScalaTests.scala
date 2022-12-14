package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

class SipScalaTests extends ScalaCliSuite {

  def noDirectoriesCommandTest(binaryName: String): Unit =
    TestInputs.empty.fromRoot { root =>
      val cliPath = os.Path(TestUtil.cliPath, os.pwd)

      os.proc(cliPath, "directories").call(cwd = root)
      os.proc(cliPath, "compile", "--help").call(cwd = root)
      os.proc(cliPath, "run", "--help").call(cwd = root)

      os.copy(cliPath, root / binaryName)
      os.perms.set(root / binaryName, "rwxr-xr-x")

      os.proc(root / binaryName, "compile", "--help").call(cwd = root)
      os.proc(root / binaryName, "run", "--help").call(cwd = root)

      val res = os.proc(root / binaryName, "directories").call(
        cwd = root,
        check = false,
        mergeErrIntoOut = true
      )
      expect(res.exitCode == 1)
      val output = res.out.text()
      expect(output.contains(s"directories: not found"))

    }

  def noPublishDirectives(): Unit = TestInputs.empty.fromRoot { root =>
    val code =
      """
        | //> using publish.name "my-library"
        | class A
        |""".stripMargin

    val source = root / "A.scala"
    os.write(source, code)

    val cliPath = os.Path(TestUtil.cliPath, os.pwd)

    os.proc(cliPath, "compile", source).call(cwd = root)

    os.copy(cliPath, root / "scala")
    os.perms.set(root / "scala", "rwxr-xr-x")

    val res = os.proc(root / "scala", "compile", source).call(
      cwd = root,
      check = false,
      mergeErrIntoOut = true
    )
    expect(res.exitCode == 1)
    val output = res.out.text()
    expect(output.contains(s"directive is not supported"))
  }

  def noMarkdownOptions(): Unit = TestInputs.empty.fromRoot { root =>
    val code =
      """
        | println("ala")
        |""".stripMargin

    val source = root / "A.sc"
    os.write(source, code)

    val cliPath = os.Path(TestUtil.cliPath, os.pwd)

    os.proc(cliPath, "--markdown", source).call(cwd = root)

    os.copy(cliPath, root / "scala")
    os.perms.set(root / "scala", "rwxr-xr-x")

    val res = os.proc(root / "scala", "--markdown", source).call(
      cwd = root,
      check = false,
      mergeErrIntoOut = true
    )
    expect(res.exitCode == 1)
    val output = res.out.text()
    expect(output.contains(s"option is not supported"))
  }

  if (TestUtil.isNativeCli && !Properties.isWin) {
    test("no directories command when run as scala") {
      noDirectoriesCommandTest("scala")
    }
    test("no directories command when run as scala-cli-sip") {
      noDirectoriesCommandTest("scala-cli-sip")
    }

    test("no publish directives when run as scala") {
      noPublishDirectives()
    }

    test("no markdown option when run as scala") {
      noMarkdownOptions()
    }
  }

  def runVersionCommand(binaryName: String) =
    TestInputs.empty.fromRoot { root =>
      val cliPath    = os.Path(TestUtil.cliPath, os.pwd)
      val ext        = if (Properties.isWin) ".exe" else ""
      val newCliPath = root / s"$binaryName$ext"
      os.copy(cliPath, newCliPath)

      for { versionOption <- Seq("version", "-version", "--version") } {
        val version = os.proc(newCliPath, versionOption).call(check = false)
        assert(
          version.exitCode == 0,
          clues(version, version.out.text(), version.err.text(), version.exitCode)
        )
        val expectedLauncherVersion =
          if (binaryName == "scala") "Scala code runner version:"
          else "Scala CLI version:"
        expect(version.out.text().contains(expectedLauncherVersion))
        expect(version.out.text().contains(s"Scala version (default): ${Constants.defaultScala}"))
      }
    }

  def checkHelp(binaryName: String): Unit = TestInputs.empty.fromRoot { root =>
    val cliPath = os.Path(TestUtil.cliPath, os.pwd)
    val ext = if (Properties.isWin) ".exe" else ""
    val newCliPath = root / s"$binaryName$ext"
    os.copy(cliPath, newCliPath)

    for { helpOption <- Seq("help", "-help", "--help") } {
      val res = os.proc(newCliPath, helpOption).call(cwd = root)
      val restrictedFeaturesMentioned = res.out.trim().contains("package")
      if (binaryName == "scala") expect(!restrictedFeaturesMentioned)
      else expect(restrictedFeaturesMentioned)
    }
  }

  if (TestUtil.isNativeCli) {
    test("version command print detailed info run as scala") {
      runVersionCommand("scala")
    }

    test("version command print detailed info run as scala-cli") {
      runVersionCommand("scala-cli")
    }

    test("help command mentions non-restricted features only when run as scala") {
      checkHelp("scala")
    }

    test("help command mentions all core features when run as scala") {
      checkHelp("scala-cli")
    }
  }
}
