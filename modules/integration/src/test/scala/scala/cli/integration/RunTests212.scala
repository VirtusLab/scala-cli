package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

// format: off
class RunTests212 extends RunTestDefinitions(
  scalaVersionOpt = Some(Constants.scala212)
) {
  // format: on

  test("Descriptive error message for unsupported native/script configurations") {
    val inputs        = TestInputs(os.rel / "a.sc" -> "println(1)")
    val nativeVersion = "0.4.2"
    inputs.fromRoot { root =>
      val output = os.proc(
        TestUtil.cli,
        extraOptions,
        "--native",
        "a.sc",
        "--native-version",
        nativeVersion
      ).call(
        cwd = root,
        check = false,
        stderr = os.Pipe
      ).err.trim()
      expect(
        output.contains(
          s"Used Scala Native version $nativeVersion is incompatible with Scala $actualScalaVersion."
        )
      )
    }
  }
}
