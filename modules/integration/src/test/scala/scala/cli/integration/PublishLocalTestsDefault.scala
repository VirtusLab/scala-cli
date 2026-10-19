package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.TestUtil.stableStdoutOf

class PublishLocalTestsDefault extends PublishLocalTestDefinitions with TestDefault {
  test(
    s"publish local --cross $actualScalaVersion with ${Constants.scala213} and ${Constants.scala212}"
  ) {
    val expectedMessage = "Hello"
    val crossVersions   = Seq(actualScalaVersion, Constants.scala213, Constants.scala212)
    PublishTestInputs.inputs(message = expectedMessage, crossVersions = Some(crossVersions))
      .fromRoot { root =>
        os.proc(
          TestUtil.cli,
          "--power",
          "publish",
          "local",
          ".",
          extraOptions,
          "--cross"
        )
          .call(cwd = root)
        def publishedDep(scalaVersionSuffix: String) =
          s"${PublishTestInputs.testOrg}:${PublishTestInputs.testName}_$scalaVersionSuffix:$testPublishVersion"
        val r3 =
          stableStdoutOf(TestUtil.cli, "run", "--dep", publishedDep("3"), extraOptions)(cwd = root)
        expect(r3 == expectedMessage)
        val r213 = stableStdoutOf(
          TestUtil.cli,
          "run",
          "--dep",
          publishedDep("2.13"),
          "-S",
          Constants.scala213,
          extraOptions
        )(cwd = root)
        expect(r213 == expectedMessage)
        val r212 = stableStdoutOf(
          TestUtil.cli,
          "run",
          "--dep",
          publishedDep("2.12"),
          "-S",
          Constants.scala212,
          extraOptions
        )(cwd = root)
        expect(r212 == expectedMessage)
      }
  }
}
