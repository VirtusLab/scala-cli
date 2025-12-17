package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

class RunTests3NextRc extends RunTestDefinitions with Test3NextRc {
  test("Scala 3.nightly & 3.<latest-minor>.nightly point to the same version") {
    TestInputs.empty.fromRoot { root =>
      def getScalaVersion(scalaVersionIndex: String) =
        os.proc(
          TestUtil.cli,
          "run",
          "-e",
          s"""println($retrieveScalaVersionCode)""",
          "-S",
          scalaVersionIndex,
          TestUtil.extraOptions
        )
          .call(cwd = root)
          .out
          .trim()

      val version1     = getScalaVersion("3.nightly")
      val nightlyMinor = version1.split('.').take(2).last
      val version2     = getScalaVersion(s"3.$nightlyMinor.nightly")
      expect(version1 == version2)
    }
  }
}
