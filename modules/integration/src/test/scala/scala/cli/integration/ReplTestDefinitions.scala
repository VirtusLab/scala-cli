package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.removeAnsiColors

abstract class ReplTestDefinitions extends ScalaCliSuite with TestScalaVersionArgs {
  _: TestScalaVersion =>
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
}
