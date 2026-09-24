package scala.cli.commands.fix

import caseapp.*

import scala.build.internal.Constants
import scala.cli.commands.shared.HelpGroup
import scala.cli.commands.tags

final case class ScalafixOptions(
  @Group(HelpGroup.Fix.toString)
  @Tag(tags.experimental)
  @HelpMessage("Custom path to the scalafix configuration file.")
  @Tag(tags.inShortHelp)
  scalafixConf: Option[String] = None,
  @Group(HelpGroup.Fix.toString)
  @Tag(tags.experimental)
  @HelpMessage("Pass extra argument(s) to scalafix.")
  @Tag(tags.inShortHelp)
  scalafixArg: List[String] = Nil,
  @Group(HelpGroup.Fix.toString)
  @Tag(tags.experimental)
  @HelpMessage("Run scalafix rule(s) explicitly, overriding the configuration file default.")
  @Tag(tags.inShortHelp)
  scalafixRules: List[String] = Nil,
  @Group(HelpGroup.Fix.toString)
  @Tag(tags.experimental)
  @HelpMessage(
    s"Pass scalafix version before running it (${Constants.scalafixVersion} by default)."
  )
  @Tag(tags.inShortHelp)
  scalafixVersion: Option[String] = None
)
object ScalafixOptions {
  implicit lazy val parser: Parser[ScalafixOptions] = Parser.derive
  implicit lazy val help: Help[ScalafixOptions]     = Help.derive
}
