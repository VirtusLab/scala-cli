package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class ScalafixTests extends ScalaCliSuite {
  override def group: ScalaCliSuite.TestGroup = ScalaCliSuite.TestGroup.First

  val confFileName = ".scalafix.conf"

  val emptyInputs: TestInputs = TestInputs(os.rel / ".placeholder" -> "")

  val simpleInputsOriginalContent: String =
    """package foo
      |
      |final object Hello {
      |  def main(args: Array[String]): Unit = {
      |    println("Hello")
      |  }
      |}
      |""".stripMargin
  val simpleInputs: TestInputs = TestInputs(
    os.rel / confFileName ->
      s"""|rules = [
          |  RedundantSyntax
          |]
          |""".stripMargin,
    os.rel / "Hello.scala" -> simpleInputsOriginalContent
  )
  val expectedSimpleInputsRewrittenContent: String = noCrLf {
    """package foo
      |
      |object Hello {
      |  def main(args: Array[String]): Unit = {
      |    println("Hello")
      |  }
      |}
      |""".stripMargin
  }

  private def noCrLf(input: String): String =
    input.replaceAll("\r\n", "\n")

  test("simple") {
    simpleInputs.fromRoot { root =>
      os.proc(TestUtil.cli, "scalafix", ".").call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Hello.scala"))
      expect(updatedContent == expectedSimpleInputsRewrittenContent)
    }
  }

  test("with --check") {
    simpleInputs.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "scalafix", "--check", ".").call(cwd = root, check = false)
      expect(res.exitCode == 1)
      val updatedContent = noCrLf(os.read(root / "Hello.scala"))
      expect(updatedContent == noCrLf(simpleInputsOriginalContent))
    }
  }
}
