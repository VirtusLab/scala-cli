package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.removeAnsiColors

abstract class ReplTestDefinitions extends ScalaCliSuite with TestScalaVersionArgs {
  this: TestScalaVersion =>
  protected lazy val extraOptions: Seq[String] = scalaVersionArgs ++ TestUtil.extraOptions

  protected lazy val canRunInRepl: Boolean =
    (actualScalaVersion.startsWith("3.3") &&
      actualScalaVersion.coursierVersion >= "3.3.7".coursierVersion) ||
    actualScalaVersion.startsWith("3.7") ||
    actualScalaVersion.coursierVersion >= "3.7.0-RC1".coursierVersion

  protected val dryRunPrefix: String    = "Dry run:"
  protected val runInReplPrefix: String = "Running in REPL:"

  def runInRepl(
    codeToRunInRepl: String,
    testInputs: TestInputs = TestInputs.empty
  )(f: os.CommandResult => Unit): Unit = {
    testInputs.fromRoot { root =>
      f(
        os.proc(
          TestUtil.cli,
          "repl",
          "--repl-quit-after-init",
          "--repl-init-script",
          codeToRunInRepl,
          extraOptions
        )
          .call(cwd = root)
      )
    }
  }

  def dryRun(
    testInputs: TestInputs = TestInputs.empty,
    cliOptions: Seq[String] = Seq.empty,
    useExtraOptions: Boolean = true
  ): Unit = {
    testInputs.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "repl",
        "--repl-dry-run",
        cliOptions,
        if useExtraOptions then extraOptions else Seq.empty
      )
        .call(cwd = root)
    }
  }

  test(s"$dryRunPrefix default")(dryRun())

  test(s"$dryRunPrefix with main scope sources") {
    dryRun(
      TestInputs(
        os.rel / "Example.scala" ->
          """object Example extends App {
            |  println("Hello")
            |}
            |""".stripMargin
      )
    )
  }

  test(s"$dryRunPrefix with main and test scope sources, and the --test flag") {
    dryRun(
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
      )
    )
  }

  test(s"$dryRunPrefix calling repl with a directory with no scala artifacts") {
    dryRun(TestInputs(os.rel / "Testing.java" -> "public class Testing {}"))
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

  if canRunInRepl then
    test(s"$runInReplPrefix simple") {
      val expectedMessage = "1337"
      runInRepl(s"""println($expectedMessage)""")(r =>
        expect(r.out.trim() == expectedMessage)
      )
    }
}
