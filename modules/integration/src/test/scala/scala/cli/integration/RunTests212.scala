package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

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
  test("2.12.nightly") {
    TestInputs(os.rel / "sample.sc" -> "println(util.Properties.versionNumberString)").fromRoot {
      root =>
        val res =
          os.proc(
            TestUtil.cli,
            "run",
            ".",
            "-S",
            "2.12.nightly",
            TestUtil.extraOptions
          )
            .call(cwd = root)
        val version = res.out.trim()
        expect(version.startsWith("2.12"))
    }
  }
}
