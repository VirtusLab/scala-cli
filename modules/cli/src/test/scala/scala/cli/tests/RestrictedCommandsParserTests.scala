package scala.cli.tests

import caseapp.core.parser.Parser
import com.eed3si9n.expecty.Expecty.expect
import munit.FunSuite

import scala.build.Logger
import scala.build.input.ScalaCliInvokeData
import scala.cli.ScalaCli
import scala.cli.commands.RestrictedCommandsParser
import scala.cli.commands.shared.CoursierOptions

final class RestrictedCommandsParserTests extends FunSuite {
  given ScalaCliInvokeData = ScalaCliInvokeData.dummy

  private def parseWithPowerMode(power: Boolean, args: Seq[String]) = {
    val previousPowerMode = ScalaCli.allowRestrictedFeatures
    ScalaCli.setPowerMode(power)
    try
      RestrictedCommandsParser(
        Parser[CoursierOptions],
        Logger.nop,
        shouldSuppressExperimentalWarnings = true,
        shouldSuppressDeprecatedWarnings = true
      ).parse(args)
    finally ScalaCli.setPowerMode(previousPowerMode)
  }

  test("experimental option outside power mode is not reported as an unrecognized argument") {
    parseWithPowerMode(power = false, Seq("--offline")) match {
      case Left(error) =>
        expect(error.message.startsWith("The `--offline` option is experimental."))
        expect(!error.message.contains("Unrecognized argument"))
      case Right(res) => fail(s"Expected a parsing error, got $res")
    }
  }

  test("experimental option is accepted in power mode") {
    val res = parseWithPowerMode(power = true, Seq("--offline"))
    expect(res.map(_._1.offline) == Right(Some(true)))
  }
}
