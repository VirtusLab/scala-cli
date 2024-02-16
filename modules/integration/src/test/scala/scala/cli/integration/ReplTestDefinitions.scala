package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.removeAnsiColors
import scala.util.Properties

abstract class ReplTestDefinitions(val scalaVersionOpt: Option[String])
    extends ScalaCliSuite with TestScalaVersionArgs {

  private lazy val extraOptions = scalaVersionArgs ++ TestUtil.extraOptions

  private val retrieveScalaVersionCode = if (actualScalaVersion.startsWith("2."))
    "scala.util.Properties.versionNumberString"
  else "dotty.tools.dotc.config.Properties.simpleVersionString"

  def expectedAmmoniteVersion: String =
    actualScalaVersion match {
      case s
          if s.startsWith("2.12") &&
          Constants.maxAmmoniteScala212Version.coursierVersion < s.coursierVersion =>
        Constants.maxAmmoniteScala212Version
      case s
          if s.startsWith("2.13") &&
          Constants.maxAmmoniteScala213Version.coursierVersion < s.coursierVersion =>
        Constants.maxAmmoniteScala213Version
      case s
          if s.startsWith("3") &&
          Constants.maxAmmoniteScala3Version.coursierVersion < s.coursierVersion =>
        Constants.maxAmmoniteScala3Version
      case s => s
    }

  test("default dry run") {
    TestInputs.empty.fromRoot { root =>
      os.proc(TestUtil.cli, "repl", extraOptions, "--repl-dry-run").call(cwd = root)
    }
  }

  def ammoniteTest(): Unit = {
    TestInputs.empty.fromRoot { root =>
      val ammArgs = Seq(
        "-c",
        s"""println("Hello" + " from Scala " + $retrieveScalaVersionCode)"""
      )
        .map {
          if (Properties.isWin)
            a => if (a.contains(" ")) "\"" + a.replace("\"", "\\\"") + "\"" else a
          else
            identity
        }
        .flatMap(arg => Seq("--ammonite-arg", arg))
      val res =
        os.proc(TestUtil.cli, "--power", "repl", extraOptions, "--ammonite", ammArgs).call(cwd =
          root
        )
      val output = res.out.trim()
      expect(output == s"Hello from Scala $expectedAmmoniteVersion")
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
        s"""println("Hello" + " from Scala " + $retrieveScalaVersionCode)
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
        "--power",
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
        "--power",
        "repl",
        extraOptions,
        "--ammonite",
        "--python",
        ammArgs
      ).call(cwd = root)
      val lines = res.out.trim().linesIterator.toVector
      expect(lines == Seq(s"Hello from Scala $expectedAmmoniteVersion", "Hello from ScalaPy"))
    }
  }

  test("default values in help") {
    TestInputs.empty.fromRoot { root =>
      val res   = os.proc(TestUtil.cli, "--power", "repl", extraOptions, "--help").call(cwd = root)
      val lines = removeAnsiColors(res.out.trim()).linesIterator.toVector

      val scalaVersionHelp = lines.find(_.contains("--scala-version")).getOrElse("")
      val ammVersionHelp   = lines.find(_.contains("--ammonite-ver")).getOrElse("")

      expect(scalaVersionHelp.contains(s"(${Constants.defaultScala} by default)"))
      expect(ammVersionHelp.contains(s"(${Constants.ammoniteVersion} by default)"))
    }
  }
}
