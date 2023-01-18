package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect
import coursier.core.Version

class DependencyUpdateTests extends ScalaCliSuite {

  test("dependency update test") {
    val fileName = "Hello.scala"
    val message  = "Hello World"
    val fileContent =
      s"""|//> using lib "com.lihaoyi::os-lib:0.7.8"
          |//> using lib "com.lihaoyi::utest:0.7.10"
          |import $$ivy.`com.lihaoyi::geny:0.6.5`
          |import $$dep.`com.lihaoyi::pprint:0.6.6`
          |
          |object Hello extends App {
          |  println("$message")
          |}""".stripMargin
    val inputs = TestInputs(os.rel / fileName -> fileContent)
    inputs.fromRoot { root =>
      // update dependencies
      val p = os.proc(TestUtil.cli, "dependency-update", "--all", fileName)
        .call(
          cwd = root,
          stdin = os.Inherit,
          mergeErrIntoOut = true
        )
      expect(p.out.trim().contains("Updated dependency"))
      expect( // check if dependency update command modify file
        os.read(root / fileName) != fileContent)

      // after updating dependencies app should run
      val out = os.proc(TestUtil.cli, fileName).call(cwd = root).out.trim()
      expect(out == message)
    }
  }

  test("update toolkit dependence") {
    val toolkitVersion = "0.1.3"
    val testInputs = TestInputs(
      os.rel / "Foo.scala" ->
        s"""//> using toolkit "$toolkitVersion"
           |
           |object Hello extends App {
           |  println("Hello")
           |}
           |""".stripMargin
    )
    testInputs.fromRoot { root =>
      // update toolkit
      os.proc(TestUtil.cli, "dependency-update", "--all", ".")
        .call(cwd = root)

      val toolkitDirective = "//> using toolkit \"(.*)\"".r
      val updatedToolkitVersionOpt = {
        val regexMatch = toolkitDirective.findFirstMatchIn(os.read(root / "Foo.scala"))
        regexMatch.map(_.group(1))
      }
      expect(updatedToolkitVersionOpt.nonEmpty)
      val updatedToolkitVersion = updatedToolkitVersionOpt.get
      expect(Version(updatedToolkitVersion) > Version(toolkitVersion))
    }
  }
}
