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
  }
}
