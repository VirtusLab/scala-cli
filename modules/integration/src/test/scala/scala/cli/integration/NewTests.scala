package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class NewTests extends ScalaCliSuite {
  override def group: ScalaCliSuite.TestGroup = ScalaCliSuite.TestGroup.First

  val simpleTemplateName = "VirtusLab/scala-cli.g8"

  val expectedTemplateContent =
    """
      |object HelloWorld {
      |  def main(args: Array[String]): Unit = {
      |    println("Hello, world!")
      |  }
      |}""".stripMargin

  val simpleTemplate: TestInputs = TestInputs(
    os.rel / "HelloWorld.scala" -> expectedTemplateContent
  )

  test("simple new template") {
    simpleTemplate.fromRoot { root =>
      os.proc(TestUtil.cli, "new", simpleTemplateName).call(cwd = root)
      val content = os.read(root / "HelloWorld.scala")
      expect(content == expectedTemplateContent)
    }
  }

  test("error without template name") {
    val output = os.proc(TestUtil.cli, "new")
    expect(output == output)
  }

}
