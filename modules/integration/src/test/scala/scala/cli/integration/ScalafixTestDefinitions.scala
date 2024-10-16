package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

abstract class ScalafixTestDefinitions extends ScalaCliSuite with TestScalaVersionArgs {
  _: TestScalaVersion =>
  override def group: ScalaCliSuite.TestGroup = ScalaCliSuite.TestGroup.First

  protected val confFileName: String = ".scalafix.conf"

  protected val unusedRuleOption: String

  protected def noCrLf(input: String): String =
    input.replaceAll("\r\n", "\n")

  private val simpleInputsOriginalContent: String =
    """package foo
      |
      |final object Hello {
      |  def main(args: Array[String]): Unit = {
      |    println("Hello")
      |  }
      |}
      |""".stripMargin
  private val simpleInputs: TestInputs = TestInputs(
    os.rel / confFileName ->
      s"""|rules = [
          |  RedundantSyntax
          |]
          |""".stripMargin,
    os.rel / "Hello.scala" -> simpleInputsOriginalContent
  )
  private val expectedContent: String = noCrLf {
    """package foo
      |
      |object Hello {
      |  def main(args: Array[String]): Unit = {
      |    println("Hello")
      |  }
      |}
      |""".stripMargin
  }

  test("simple") {
    simpleInputs.fromRoot { root =>
      os.proc(TestUtil.cli, "scalafix", ".", "--power", scalaVersionArgs).call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Hello.scala"))
      expect(updatedContent == expectedContent)
    }
  }

  test("with --check") {
    simpleInputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "scalafix", "--power", "--check", ".", scalaVersionArgs).call(
        cwd = root,
        check = false
      )
      expect(res.exitCode != 0)
      val updatedContent = noCrLf(os.read(root / "Hello.scala"))
      expect(updatedContent == noCrLf(simpleInputsOriginalContent))
    }
  }

  test("semantic rule") {
    val unusedValueInputsContent: String =
      s"""//> using options $unusedRuleOption
         |package foo
         |
         |object Hello {
         |  def main(args: Array[String]): Unit = {
         |    val name = "John"
         |    println("Hello")
         |  }
         |}
         |""".stripMargin
    val semanticRuleInputs: TestInputs = TestInputs(
      os.rel / confFileName ->
        s"""|rules = [
            |  RemoveUnused
            |]
            |""".stripMargin,
      os.rel / "Hello.scala" -> unusedValueInputsContent
    )
    val expectedContent: String = noCrLf {
      s"""//> using options $unusedRuleOption
         |package foo
         |
         |object Hello {
         |  def main(args: Array[String]): Unit = {
         |    
         |    println("Hello")
         |  }
         |}
         |""".stripMargin
    }

    semanticRuleInputs.fromRoot { root =>
      os.proc(TestUtil.cli, "scalafix", "--power", ".", scalaVersionArgs).call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Hello.scala"))
      expect(updatedContent == expectedContent)
    }
  }

  test("rule args") {
    val input = TestInputs(
      os.rel / confFileName ->
        s"""|rules = [
            |  RemoveUnused,
            |  ExplicitResultTypes
            |]
            |""".stripMargin,
      os.rel / "Hello.scala" ->
        s"""|//> using options $unusedRuleOption
            |package hello
            |
            |object Hello {
            |  def a = {
            |    val x = 1 // keep unused - exec only ExplicitResultTypes
            |    42
            |  }
            |}
            |""".stripMargin
    )

    input.fromRoot { root =>
      os.proc(
        TestUtil.cli,
        "scalafix",
        ".",
        "--rules",
        "ExplicitResultTypes",
        "--power",
        scalaVersionArgs
      ).call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Hello.scala"))
      val expected =
        s"""|//> using options $unusedRuleOption
            |package hello
            |
            |object Hello {
            |  def a: Int = {
            |    val x = 1 // keep unused - exec only ExplicitResultTypes
            |    42
            |  }
            |}
            |""".stripMargin

      expect(updatedContent == expected)

    }
  }
}
