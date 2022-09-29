package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.util.Properties

abstract class ReplTestDefinitions(val scalaVersionOpt: Option[String])
    extends ScalaCliSuite with TestScalaVersionArgs {

  private lazy val extraOptions = scalaVersionArgs ++ TestUtil.extraOptions

  protected def versionNumberString: String =
    if (actualScalaVersion.startsWith("2.")) actualScalaVersion
    // Scala 3 gives the 2.13 version it depends on for its standard library.
    // Assuming it's the same Scala 3 version as the integration tests here.
    else Properties.versionNumberString

  test("default dry run") {
    TestInputs.empty.fromRoot { root =>
      os.proc(TestUtil.cli, "repl", extraOptions, "--repl-dry-run").call(cwd = root)
    }
  }

  def ammoniteTest(): Unit = {
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

  // Temporarily filtering out 3.2.0, until com-lihaoyi/Ammonite#1286 is merged.
  if (actualScalaVersion != "3.2.0")
    test("ammonite") {
      ammoniteTest()
    }

  test("ammonite scalapy") {
    TestInputs.empty.fromRoot { root =>
      val ammArgs = Seq(
        "-c",
        """println("Hello" + " from Scala " + scala.util.Properties.versionNumberString)
          |// py.Dynamic.global.print("Hello from", "ScalaPy") // doesn't work
          |println(py"'Hello from '" + py"'ScalaPy'")
          |""".stripMargin
      )
        .map {
          if (Properties.isWin)
            a => if (a.contains(" ")) "\"" + a.replace("\"", "\\\"") + "\"" else a
          else
            identity
        }
        .flatMap(arg => Seq("--ammonite-arg", arg))
      val res = os.proc(
        TestUtil.cli,
        "repl",
        "-v",
        "-v",
        extraOptions,
        "--ammonite",
        "--python",
        ammArgs
      ).call(cwd = root)
      val lines = res.out.trim().linesIterator.toVector
      expect(lines == Seq(s"Hello from Scala $versionNumberString", "Hello from ScalaPy"))
    }
  }

}
