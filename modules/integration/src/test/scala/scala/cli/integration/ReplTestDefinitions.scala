package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

abstract class ReplTestDefinitions(val scalaVersionOpt: Option[String])
    extends munit.FunSuite with TestScalaVersionArgs {

  private lazy val extraOptions = scalaVersionArgs ++ TestUtil.extraOptions

  protected def versionNumberString = actualScalaVersion

  test("default dry run") {
    TestInputs(Nil).fromRoot { root =>
      os.proc(TestUtil.cli, "repl", extraOptions, "--repl-dry-run").call(cwd = root)
    }
  }

  test("ammonite") {
    TestInputs(Nil).fromRoot { root =>
      val ammArgs = Seq(
        "-c",
        """println("Hello" + " from Scala " + scala.util.Properties.versionNumberString)"""
      )
        .map {
          if (Properties.isWin)
            a => if (a.contains(" ")) "\"" + a.replace("\"", "\\\"") + "\"" else a
          else
            identity
        }
        .flatMap(arg => Seq("--ammonite-arg", arg))
      val res = os.proc(TestUtil.cli, "repl", extraOptions, "--ammonite", ammArgs).call(cwd = root)
      val output = res.out.text().trim
      expect(output == s"Hello from Scala $versionNumberString")
    }
  }

}
