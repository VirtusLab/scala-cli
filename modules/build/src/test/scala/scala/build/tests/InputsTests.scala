package scala.build.tests

import com.eed3si9n.expecty.Expecty.expect

class InputsTests extends munit.FunSuite {

  test("forced workspace") {
    val testInputs = TestInputs(
      os.rel / "Foo.scala" ->
        """object Foo {
          |  def main(): Unit = {
          |    println("Hello")
          |  }
          |}
          |""".stripMargin
    )
    val forcedWorkspace = os.rel / "workspace"
    testInputs.withCustomInputs(viaDirectory = false, forcedWorkspaceOpt = Some(forcedWorkspace)) {
      (root, inputs) =>
        expect(inputs.workspace == root / forcedWorkspace)
    }
  }

}
