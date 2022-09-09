package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

abstract class ReplTestDefinitions(val scalaVersionOpt: Option[String])
    extends ScalaCliSuite with TestScalaVersionArgs {

  private lazy val extraOptions = scalaVersionArgs ++ TestUtil.extraOptions

  protected def versionNumberString: String = actualScalaVersion

  test("default dry run") {
    TestInputs.empty.fromRoot { root =>
      os.proc(TestUtil.cli, "repl", extraOptions, "--repl-dry-run").call(cwd = root)
    }
  }

  test("repl custom repositories work") {
    TestInputs.empty.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "repl",
        "--repl-dry-run",
        "--scala",
        Constants.scalaSnapshot213,
        "--repository",
        "https://scala-ci.typesafe.com/artifactory/scala-integration"
      ).call(cwd = root)
    }
  }

  test("ammonite") {
    TestInputs.empty.fromRoot { root =>
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
      val output = res.out.trim()
      expect(output == s"Hello from Scala $versionNumberString")
    }
  }

}
