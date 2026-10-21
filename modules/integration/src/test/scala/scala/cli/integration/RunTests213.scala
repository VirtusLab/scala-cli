package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.stableStdoutOf

class RunTests213 extends RunTestDefinitions with Test213 {
  test("ensure typesafe PR validation snapshot Scala versions are supported".flaky) {
    TestInputs(os.rel / "sample.sc" -> "println(util.Properties.versionNumberString)").fromRoot {
      root =>
        val snapshotScalaVersion = "2.13.11-bin-89f3de5-SNAPSHOT"
        val res                  =
          stableStdoutOf(
            TestUtil.cli,
            "run",
            ".",
            "--repository",
            "https://scala-ci.typesafe.com/artifactory/scala-pr-validation-snapshots",
            "-S",
            snapshotScalaVersion,
            TestUtil.extraOptions
          )(cwd = root)
        expect(res == "2.13.11-20221128-143922-89f3de5")
    }
  }
  test("2.13.nightly".flaky) {
    TestInputs(os.rel / "sample.sc" -> "println(util.Properties.versionNumberString)").fromRoot {
      root =>
        val res =
          stableStdoutOf(
            TestUtil.cli,
            "run",
            ".",
            "-S",
            "2.13.nightly",
            TestUtil.extraOptions
          )(cwd = root)
        val version = res
        expect(version.startsWith("2.13"))
    }
  }
}
