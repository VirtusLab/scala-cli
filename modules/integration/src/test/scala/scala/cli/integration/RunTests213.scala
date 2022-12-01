package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

// format: off
class RunTests213 extends RunTestDefinitions(
  scalaVersionOpt = Some(Constants.scala213)
){
// format: on
  test("ensure typesafe PR validation snapshot Scala versions are supported") {
    TestInputs(os.rel / "sample.sc" -> "println(util.Properties.versionNumberString)").fromRoot {
      root =>
        val snapshotScalaVersion = "2.13.11-bin-89f3de5-SNAPSHOT"
        val res =
          os.proc(
            TestUtil.cli,
            "run",
            ".",
            "--repository",
            "https://scala-ci.typesafe.com/artifactory/scala-pr-validation-snapshots",
            "-S",
            snapshotScalaVersion,
            TestUtil.extraOptions
          )
            .call(cwd = root)
        expect(res.out.trim() == "2.13.11-20221128-143922-89f3de5")
    }
  }
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
