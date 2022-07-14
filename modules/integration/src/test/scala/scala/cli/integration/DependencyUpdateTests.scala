package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class DependencyUpdateTests extends munit.FunSuite {

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
    val inputs = TestInputs(
      Seq(
        os.rel / fileName -> fileContent
      )
    )
    inputs.fromRoot { root =>
      // update dependencies
      val p = os.proc(TestUtil.cli, "dependency-update", "--all", fileName)
        .call(
          cwd = root,
          stdin = os.Inherit,
          mergeErrIntoOut = true
        )
      expect(p.out.text().trim.contains("Updated dependency to"))
      expect( // check if dependency update command modify file
        os.read(root / fileName) != fileContent)

      // after updating dependencies app should run
      val out = os.proc(TestUtil.cli, fileName).call(cwd = root).out.text().trim
      expect(out == message)
    }
  }
}
