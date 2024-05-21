package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

trait CompileTests3StableDefinitions { _: CompileTestDefinitions =>
  test(s"TASTY processor does not warn about Scala $actualScalaVersion") {
    TestInputs(os.rel / "simple.sc" -> s"""println("Hello")""")
      .fromRoot { root =>
        val result =
          os.proc(TestUtil.cli, "compile", ".", extraOptions)
            .call(cwd = root, stderr = os.Pipe)
        expect(result.exitCode == 0)
        expect(!result.err.text().contains("cannot post process TASTY files"))
      }
  }

}
