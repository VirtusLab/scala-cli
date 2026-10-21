package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.stableStdoutOf

class RunTests212 extends RunTestDefinitions with Test212 {
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
  test("2.12.nightly".flaky) {
    TestInputs(os.rel / "sample.sc" -> "println(util.Properties.versionNumberString)").fromRoot {
      root =>
        val res =
          stableStdoutOf(
            TestUtil.cli,
            "run",
            ".",
            "-S",
            "2.12.nightly",
            TestUtil.extraOptions
          )(cwd = root)
        val version = res
        expect(version.startsWith("2.12"))
    }
  }
}
