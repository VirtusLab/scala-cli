package cli.tests
import com.eed3si9n.expecty.Expecty.expect
import dependency.ScalaParameters

import scala.build.internal.Constants
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

  val expectedScalaCliVersions = Seq(
    "0.1.2"                       -> Constants.defaultScala212Version,
    "0.1.1+43-g15666b67-SNAPSHOT" -> Constants.defaultScala212Version,
    "0.1.3"                       -> Constants.defaultScala213Version,
    "nightly"                     -> Properties.versionNumberString
  )

  for ((cliVersion, expectedScalaVersion) <- expectedScalaCliVersions)
    test(s"use expected scala version for JVM Scala CLi launcher: $cliVersion") {
      val scalaVersion = LauncherCli.scalaCliVersion(cliVersion, None)

      expect(scalaVersion == expectedScalaVersion)
    }

}
