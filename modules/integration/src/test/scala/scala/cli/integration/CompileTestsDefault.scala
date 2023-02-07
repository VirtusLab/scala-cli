package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect
class CompileTestsDefault extends CompileTestDefinitions(scalaVersionOpt = None) {
  test("render explain message") {
    val fileName = "Hello.scala"
    val inputs = TestInputs(
      os.rel / fileName -> // should be dump to 3.3.1 after release
        s"""//> using scala "3.3.1-RC1-bin-20230203-3ef1e73-NIGHTLY"
           |//> using options "--explain"
           |
           |class A
           |val i: Int = A()
           |""".stripMargin
    )
    inputs.fromRoot { root =>
      val out = os.proc(TestUtil.cli, "compile", extraOptions, fileName)
        .call(cwd = root, check = false, mergeErrIntoOut = true).out.trim()

      expect(out.contains("Explanation"))
    }
  }
}
