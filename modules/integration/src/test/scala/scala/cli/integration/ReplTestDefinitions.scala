package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.removeAnsiColors
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

  test("ammonite") {
    ammoniteTest()
  }

  test("ammonite scalapy") {
    val inputs = TestInputs(
      os.rel / "foo" / "something.py" ->
        """messageStart = 'Hello from'
          |messageEnd = 'ScalaPy'
          |""".stripMargin
    )
    inputs.fromRoot { root =>
      val ammArgs = Seq(
        "-c",
        """println("Hello" + " from Scala " + scala.util.Properties.versionNumberString)
          |val sth = py.module("foo.something")
          |py.Dynamic.global.applyDynamicNamed("print")("" -> sth.messageStart, "" -> sth.messageEnd, "flush" -> py.Any.from(true))
          |""".stripMargin
      )
        .map {
          if (Properties.isWin)
            a => if (a.contains(" ")) "\"" + a.replace("\"", "\\\"") + "\"" else a
          else
            identity
        }
        .flatMap(arg => Seq("--ammonite-arg", arg))

      val errorRes = os.proc(
        TestUtil.cli,
        "repl",
        extraOptions,
        "--ammonite",
        "--python",
        ammArgs
      ).call(
        cwd = root,
        env = Map("PYTHONSAFEPATH" -> "foo"),
        mergeErrIntoOut = true,
        check = false
      )
      expect(errorRes.exitCode != 0)
      val errorOutput = errorRes.out.text()
      expect(errorOutput.contains("No module named 'foo'"))

      val res = os.proc(
        TestUtil.cli,
        "repl",
        extraOptions,
        "--ammonite",
        "--python",
        ammArgs
      ).call(cwd = root)
      val lines = res.out.trim().linesIterator.toVector
      expect(lines == Seq(s"Hello from Scala $versionNumberString", "Hello from ScalaPy"))
    }
  }

  test("default values in help") {
    TestInputs.empty.fromRoot { root =>
      val res   = os.proc(TestUtil.cli, "repl", extraOptions, "--help").call(cwd = root)
      val lines = removeAnsiColors(res.out.trim()).linesIterator.toVector

      val scalaVersionHelp   = lines.find(_.contains("--scala-version")).getOrElse("")
      val scalaPyVersionHelp = lines.find(_.contains("--scalapy-version")).getOrElse("")
      val ammVersionHelp     = lines.find(_.contains("--ammonite-ver")).getOrElse("")

      expect(scalaVersionHelp.contains(s"(${Constants.defaultScala} by default)"))
      expect(scalaPyVersionHelp.contains(s"(${Constants.scalaPyVersion} by default)"))
      expect(ammVersionHelp.contains(s"(${Constants.ammoniteVersion} by default)"))
    }
  }
}
