package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class ScalafixTests extends ScalaCliSuite {
  override def group: ScalaCliSuite.TestGroup = ScalaCliSuite.TestGroup.First

  val confFileName = ".scalafix.conf"

  val emptyInputs: TestInputs = TestInputs(os.rel / ".placeholder" -> "")

  val simpleInputsScala3OriginalContent: String =
    """package foo
      |
      |final object Hello:
      |  def main(args: Array[String]): Unit =
      |    println("Hello")
      |""".stripMargin
  val simpleInputs3: TestInputs = TestInputs(
    os.rel / confFileName ->
      s"""|rules = [
          |  RedundantSyntax
          |]
          |""".stripMargin,
    os.rel / "Hello.scala" -> simpleInputsScala3OriginalContent
  )

  private def noCrLf(input: String): String =
    input.replaceAll("\r\n", "\n")

  test("simple for scala2 code") {
    val simpleInputsScala2OriginalContent: String =
      """package foo
        |
        |final object Hello {
        |  def main(args: Array[String]): Unit = {
        |    println("Hello")
        |  }
        |}
        |""".stripMargin
    val simpleInputs2: TestInputs = TestInputs(
      os.rel / confFileName ->
        s"""|rules = [
            |  RedundantSyntax
            |]
            |""".stripMargin,
      os.rel / "Hello.scala" -> simpleInputsScala2OriginalContent
    )
    val expectedContent: String = noCrLf {
      """package foo
        |
        |object Hello {
        |  def main(args: Array[String]): Unit = {
        |    println("Hello")
        |  }
        |}
        |""".stripMargin
    }

    simpleInputs2.fromRoot { root =>
      os.proc(TestUtil.cli, "scalafix", ".", "-S", "2", "--power").call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Hello.scala"))
      expect(updatedContent == expectedContent)
    }
  }

  test("simple for scala3 code") {
    val expectedContent: String = noCrLf {
      """package foo
        |
        |object Hello:
        |  def main(args: Array[String]): Unit =
        |    println("Hello")
        |""".stripMargin
    }

    simpleInputs3.fromRoot { root =>
      os.proc(TestUtil.cli, "scalafix", ".", "-S", "3", "--power").call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Hello.scala"))
      expect(updatedContent == expectedContent)
    }
  }

  test("with --check") {
    simpleInputs3.fromRoot { root =>
      val res = os.proc(TestUtil.cli, "scalafix", "--power", "--check", ".").call(cwd = root, check = false)
      expect(res.exitCode == 1)
      val updatedContent = noCrLf(os.read(root / "Hello.scala"))
      expect(updatedContent == noCrLf(simpleInputsScala3OriginalContent))
    }
  }

  test("semantic rule") {
    val unusedValueInputsContent: String =
      """//> using options -Wunused:all
        |package foo
        |
        |object Hello:
        |  def main(args: Array[String]): Unit =
        |    val name = "John"
        |    println("Hello")
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
      """//> using options -Wunused:all
        |package foo
        |
        |object Hello:
        |  def main(args: Array[String]): Unit =
        |
        |    println("Hello")
        |""".stripMargin
    }

    semanticRuleInputs.fromRoot { root =>
      os.proc(TestUtil.cli, "scalafix", "--power", ".").call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Hello.scala"))
      expect(updatedContent == expectedContent)
    }
  }
  test("external rule") {
    val unnamedParamsInputsContent: String =
      """//> using options -P:semanticdb:synthetics:on
        |//> using compileOnly.dep "com.github.jatcwang::scalafix-named-params:0.2.4"
        |
        |package foo
        |
        |object Hello {
        |  def greetMany(name: String, times: Int) =
        |    for {
        |      i <- 0 to times
        |      _ = println(s"Hello $name")
        |    } yield ()
        |
        |  def main(args: Array[String]): Unit =
        |    greetMany("John", 42)
        |}
        |""".stripMargin
    val externalRuleInputs: TestInputs = TestInputs(
      os.rel / confFileName ->
        s"""|rules = [
            |  UseNamedParameters
            |]
            |
            |UseNamedParameters.minParams = 2
            |""".stripMargin,
      os.rel / "Hello.scala" -> unnamedParamsInputsContent
    )
    val expectedContent: String = noCrLf {
      """//> using options -P:semanticdb:synthetics:on
        |//> using compileOnly.dep "com.github.jatcwang::scalafix-named-params:0.2.4"
        |
        |package foo
        |
        |object Hello {
        |  def greetMany(name: String, times: Int) =
        |    for {
        |      i <- 0 to times
        |      _ = println(s"Hello $name")
        |    } yield ()
        |
        |  def main(args: Array[String]): Unit =
        |    greetMany(name = "John", times = 42)
        |}
        |""".stripMargin
    }

    externalRuleInputs.fromRoot { root =>
      os.proc(TestUtil.cli, "scalafix", "--power", ".", "-S", "2").call(cwd = root)
      val updatedContent = noCrLf(os.read(root / "Hello.scala"))
      println(updatedContent)
      println(expectedContent)
      expect(updatedContent == expectedContent)
    }
  }
}
