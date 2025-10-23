package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.removeAnsiColors

abstract class ReplTestDefinitions extends ScalaCliSuite with TestScalaVersionArgs {
  this: TestScalaVersion =>
  protected lazy val extraOptions: Seq[String] = scalaVersionArgs ++ TestUtil.extraOptions

  test("dry run (default)") {
    TestInputs.empty.fromRoot { root =>
      os.proc(TestUtil.cli, "repl", extraOptions, "--repl-dry-run").call(cwd = root)
    }
  }

  test("dry run (with main scope sources)") {
    TestInputs(
      os.rel / "Example.scala" ->
        """object Example extends App {
          |  println("Hello")
          |}
          |""".stripMargin
    ).fromRoot { root =>
      os.proc(TestUtil.cli, "repl", ".", extraOptions, "--repl-dry-run").call(cwd = root)
    }
  }

  test("dry run (with main and test scope sources, and the --test flag)") {
    TestInputs(
      os.rel / "Example.scala" ->
        """object Example extends App {
          |  println("Hello")
          |}
          |""".stripMargin,
      os.rel / "Example.test.scala" ->
        s"""//> using dep org.scalameta::munit::${Constants.munitVersion}
           |
           |class Example extends munit.FunSuite {
           |  test("is true true") { assert(true) }
           |}
           |""".stripMargin
    ).fromRoot { root =>
      os.proc(TestUtil.cli, "repl", ".", extraOptions, "--repl-dry-run", "--test").call(cwd = root)
    }
  }

  test("default scala version in help") {
    TestInputs.empty.fromRoot { root =>
      val res              = os.proc(TestUtil.cli, "repl", extraOptions, "--help").call(cwd = root)
      val lines            = removeAnsiColors(res.out.trim()).linesIterator.toVector
      val scalaVersionHelp = lines.find(_.contains("--scala-version")).getOrElse("")
      expect(scalaVersionHelp.contains(s"(${Constants.defaultScala} by default)"))
    }
  }

  test("calling repl with -Xshow-phases flag") {
    val cmd = Seq[os.Shellable](
      TestUtil.cli,
      "repl",
      "-Xshow-phases",
      extraOptions
    )

    val res = os.proc(cmd).call(mergeErrIntoOut = true)
    expect(res.exitCode == 0)
    val output = res.out.text()
    expect(output.contains("parser"))
    expect(output.contains("typer"))
  }

  test("calling repl with a directory with no scala artifacts") {
    val inputs = TestInputs(
      os.rel / "Testing.java" -> "public class Testing {}"
    )
    val cmd = Seq[os.Shellable](
      TestUtil.cli,
      "repl",
      "--repl-dry-run",
      ".",
      extraOptions
    )
    inputs.fromRoot { root =>
      val res = os.proc(cmd)
        .call(cwd = root)
      expect(res.exitCode == 0)
    }
  }

  if (
    (actualScalaVersion.startsWith("3.3") &&
    actualScalaVersion.coursierVersion >= "3.3.7".coursierVersion) ||
    actualScalaVersion.startsWith("3.7") ||
    actualScalaVersion.coursierVersion >= "3.7.0-RC1".coursierVersion
  )
    test("run hello world from the REPL") {
      TestInputs.empty.fromRoot { root =>
        val expectedMessage = "1337"
        val code            = s"""println($expectedMessage)"""
        val r               = os.proc(
          TestUtil.cli,
          "repl",
          "--repl-quit-after-init",
          "--repl-init-script",
          code,
          extraOptions
        )
          .call(cwd = root)
        expect(r.out.trim() == expectedMessage)
      }
    }
}
