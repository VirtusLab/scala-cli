package scala.cli.integration.compose

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.{Constants, RunTestDefinitions, TestInputs, TestUtil}

trait ComposeRunDefinitions { _: RunTestDefinitions =>
  test("compose simple modules") {
    val inputs = TestInputs(
      os.rel / Constants.moduleConfigFileName ->
        """[modules.core]
          |dependsOn = ["utils"]
          |
          |[modules.utils]
          |roots = ["Utils.scala", "Utils2.scala"]
          |""".stripMargin,
      os.rel / "core" / "Core.scala" ->
        """object Core extends App {
          |  println(Utils.util)
          |  println(Utils2.util)
          |}
          |""".stripMargin,
      os.rel / "Utils.scala"  -> "object Utils { def util: String = \"util\"}",
      os.rel / "Utils2.scala" -> "object Utils2 { def util: String = \"util2\"}"
    )

    inputs.fromRoot { root =>
      val called =
        os.proc(TestUtil.cli, extraOptions, "--power", "core", Constants.moduleConfigFileName)
          .call(cwd = root, stderr = os.Pipe)
      expect(called.out.trim() ==
        """util
          |util2""".stripMargin)

      expect(called.err.trim().contains("Compiled core"))
      expect(called.err.trim().contains("Compiled utils"))
    }
  }
}
