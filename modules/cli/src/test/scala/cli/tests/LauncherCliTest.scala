package cli.tests
import com.eed3si9n.expecty.Expecty.expect
import dependency.ScalaParameters

import scala.build.tests.TestLogger
import scala.cli.commands.CoursierOptions
import scala.cli.commands.util.CommonOps._
import scala.cli.launcher.LauncherCli
import scala.util.Properties

class LauncherCliTest extends munit.FunSuite {

  test("resolve nightly version") {
    val logger          = TestLogger()
    val cache           = CoursierOptions().coursierCache(logger.coursierLogger(""))
    val scalaParameters = ScalaParameters(Properties.versionNumberString)

    val nightlyCliVersion = LauncherCli.resolveNightlyScalaCliVersion(cache, scalaParameters)
    expect(nightlyCliVersion.endsWith("-SNAPSHOT"))
  }

}
