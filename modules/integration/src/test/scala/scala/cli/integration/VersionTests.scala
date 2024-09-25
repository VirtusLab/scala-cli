package scala.cli.integration

import com.eed3si9n.expecty.Expecty.expect

import scala.cli.integration.VersionTests.variants

class VersionTests extends ScalaCliSuite {

  for (versionOption <- variants) {
    test(versionOption) {
      // tests if the format is correct instead of comparing to a version passed via Constants
      // in order to catch errors in Mill configuration, too
      val versionRegex = ".*\\d+[.]\\d+[.]\\d+.*".r
      val version      = os.proc(TestUtil.cli, versionOption).call(check = false)
      assert(
        versionRegex.findFirstMatchIn(version.out.text()).isDefined,
        clues(version.exitCode, version.out.text(), version.err.text())
      )
      expect(version.exitCode == 0)
    }

    test(s"$versionOption --offline") {
      TestInputs.empty.fromRoot { root =>
        // TODO: --power should not be necessary here
        os.proc(TestUtil.cli, versionOption, "--offline", "--power").call(cwd = root)
      }
    }
  }

}

object VersionTests {
  val variants = Seq("version", "-version", "--version")
}
