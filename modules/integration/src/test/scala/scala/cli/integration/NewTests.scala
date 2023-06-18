package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.removeAnsiColors
  
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
      os.proc(TestUtil.cli, "--power", "new", simpleTemplateName).call(cwd = root)
      val content = os.read(root / "HelloWorld.scala")
      expect(content == expectedTemplateContent)
    }
  }

  test("error missing template argument") {
    TestInputs.empty.fromRoot { root =>
      val result = os.proc(TestUtil.cli, "--power", "new").call(cwd = root, check = false)
      val lines = removeAnsiColors(result.err.text()).linesIterator.toVector
      assert(result.exitCode == 1)
      expect(lines.contains("Error: Missing argument <template>"))
    }
  }

}
