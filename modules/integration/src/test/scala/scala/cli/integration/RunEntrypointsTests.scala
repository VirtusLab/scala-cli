package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class RunEntrypointsTests extends munit.FunSuite {

  val extraArgs = Seq(
    // using a nightly so that experimental features can be used (required for entrypoint macro annotation)
    "--scala=3.3.0-RC2",
    // seems Bloop doesn't handle well macro annotations for now
    "--server=false"
  )

  test("simple") {
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        s"""//> using entrypoints
           |
           |@annotation.experimental
           |object Hello {
           |  @caseapp.entrypoint
           |  def hello(): Unit =
           |    println("Hello")
           |}
           |""".stripMargin
    )

    inputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "run", ".", extraArgs)
        .call(cwd = root)
      val output = res.out.trim()
      expect(output == "Hello")
    }
  }

  test("few args") {
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        s"""//> using entrypoints
           |
           |@annotation.experimental
           |object Hello {
           |  @caseapp.entrypoint
           |  def hello(
           |    n: Int,
           |    @caseapp.HelpMessage("The name")
           |      name: String
           |  ): Unit =
           |    println(s"Hello $$name ($$n)")
           |}
           |""".stripMargin
    )

    inputs.fromRoot { root =>
      val failRes = os.proc(TestUtil.cli, "run", ".", extraArgs, "--", "--name", "Alex")
        .call(cwd = root, check = false, mergeErrIntoOut = true)
      expect(failRes.exitCode != 0)
      val failOutput = failRes.out.text()
      expect(failOutput.contains("Required option -n not specified"))

      val res = os.proc(TestUtil.cli, "run", ".", extraArgs, "--", "-n", "2", "--name", "Alex")
        .call(cwd = root)
      val output = res.out.trim()
      expect(output == "Hello Alex (2)")

      val completeRes = os.proc(
        TestUtil.cli,
        "complete",
        "zsh-v1",
        (5 + extraArgs.length).toString,
        "scala-cli",
        "run",
        extraArgs,
        "Hello.scala",
        "--",
        "-"
      )
        .call(cwd = root)
      val completeLines = completeRes.out.lines()
      expect(completeLines.contains(""""--name:The name""""))
      expect(completeLines.contains(""""-n""""))
    }
  }

  test("optional arg") {
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        s"""//> using entrypoints
           |
           |@annotation.experimental
           |object Hello {
           |  @caseapp.entrypoint
           |  def hello(
           |    n: Int = 1,
           |    name: String
           |  ): Unit =
           |    println(s"Hello $$name ($$n)")
           |}
           |""".stripMargin
    )

    inputs.fromRoot { root =>
      val defaultRes = os.proc(TestUtil.cli, "run", ".", extraArgs, "--", "--name", "Alex")
        .call(cwd = root)
      val defaultOutput = defaultRes.out.trim()
      expect(defaultOutput == "Hello Alex (1)")

      val res = os.proc(TestUtil.cli, "run", ".", extraArgs, "--", "-n", "2", "--name", "Alex")
        .call(cwd = root)
      val output = res.out.trim()
      expect(output == "Hello Alex (2)")
    }
  }

  test("var args") {
    val inputs = TestInputs(
      os.rel / "Hello.scala" ->
        s"""//> using entrypoints
           |
           |@annotation.experimental
           |object Hello {
           |  @caseapp.entrypoint
           |  def hello(
           |    n: Int = 1,
           |    name: String,
           |    other: String*
           |  ): Unit =
           |    println(s"Hello $$name ($$n)" + other.map(" " + _).mkString)
           |}
           |""".stripMargin
    )

    inputs.fromRoot { root =>
      val res = os.proc(
        TestUtil.cli,
        "run",
        ".",
        extraArgs,
        "--",
        "-n",
        "2",
        "--name",
        "Alex",
        "foo",
        "something"
      )
        .call(cwd = root)
      val output = res.out.trim()
      expect(output == "Hello Alex (2) foo something")
    }
  }

}
