package scala.cli.commands.fix

import caseapp._
import coursier.core.Version

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.FetchExternalBinary
import scala.build.options.BuildOptions
import scala.cli.ScalaCli.fullRunnerName
import scala.cli.commands.shared.{HasSharedOptions, HelpGroup, HelpMessages, SharedOptions}
import scala.cli.commands.{Constants, tags}
import scala.util.Properties

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
  scalafixRules: List[String] = Nil
)
object ScalafixOptions {
  implicit lazy val parser: Parser[ScalafixOptions] = Parser.derive
  implicit lazy val help: Help[ScalafixOptions]     = Help.derive
}
