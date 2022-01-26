package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

class SipScalaTests extends munit.FunSuite {

  def noDirectoriesCommandTest(): Unit =
    TestInputs(Nil).fromRoot { root =>
      val cliPath = os.Path(TestUtil.cliPath, os.pwd)

      os.proc(cliPath, "directories").call(cwd = root)
      os.proc(cliPath, "compile", "--help").call(cwd = root)
      os.proc(cliPath, "run", "--help").call(cwd = root)

      os.copy(cliPath, root / "scala")
      os.perms.set(root / "scala", "rwxr-xr-x")

      os.proc(root / "scala", "compile", "--help").call(cwd = root)
      os.proc(root / "scala", "run", "--help").call(cwd = root)

      val res = os.proc(root / "scala", "directories").call(
        cwd = root,
        check = false,
        mergeErrIntoOut = true
      )
      expect(res.exitCode == 1)
      val output = res.out.text()
      expect(output.contains("directories: not found"))
    }

  if (TestUtil.isNativeCli && !Properties.isWin)
    test("no directories command when run as scala") {
      noDirectoriesCommandTest()
    }

}
