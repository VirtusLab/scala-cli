package scala.cli.commands.shared

import caseapp.*

import scala.cli.commands.tags

final case class GlobalSuppressWarningOptions(
  @Group(HelpGroup.SuppressWarnings.toString)
  @Tag(tags.implementation)
  @HelpMessage("Suppress warnings about using experimental features")
  @Name("suppressExperimentalWarning")
  suppressExperimentalFeatureWarning: Option[Boolean] = None
)

object GlobalSuppressWarningOptions {
  implicit lazy val parser: Parser[GlobalSuppressWarningOptions] = Parser.derive
  implicit lazy val help: Help[GlobalSuppressWarningOptions]     = Help.derive

  def shouldSuppressExperimentalFeatureWarning(args: List[String]): Option[Boolean] =
    parser
      .detailedParse(args, stopAtFirstUnrecognized = false, ignoreUnrecognized = true)
      .toOption
      .flatMap(_._1.suppressExperimentalFeatureWarning)
}
