package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

// format: off
class RunTests213 extends RunTestDefinitions(
  scalaVersionOpt = Some(Constants.scala213)
){
// format: on
  test("2.13.nightly") {
    TestInputs(os.rel / "sample.sc" -> "println(util.Properties.versionNumberString)").fromRoot {
      root =>
        val res =
          os.proc(
            TestUtil.cli,
            "run",
            ".",
            "-S",
            "2.13.nightly",
            TestUtil.extraOptions
          )
            .call(cwd = root)
        val version = res.out.trim()
        expect(version.startsWith("2.13"))
    }
  }
}
