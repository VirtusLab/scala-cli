package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class ScalafixTests213 extends ScalafixTestDefinitions with Test213 {
  override val unusedRuleOption: String = "-Wunused"

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
