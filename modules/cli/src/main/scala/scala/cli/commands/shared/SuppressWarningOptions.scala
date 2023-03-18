package scala.cli.commands.shared

import caseapp.*

import scala.cli.commands.tags

// format: off
final case class SuppressWarningOptions(
  @Group(HelpGroup.SuppressWarnings.toString)
  @Tag(tags.implementation)
  @HelpMessage("Suppress warnings about using directives in multiple files")
  @Name("suppressWarningDirectivesInMultipleFiles")
    suppressDirectivesInMultipleFilesWarning: Option[Boolean] = None,
  @Group(HelpGroup.SuppressWarnings.toString)
  @Tag(tags.implementation)
  @HelpMessage("Suppress warnings about outdated dependencies in project")
    suppressOutdatedDependencyWarning: Option[Boolean] = None,
  @Recurse
    global: GlobalSuppressWarningOptions = GlobalSuppressWarningOptions()
)
// format: on

object SuppressWarningOptions {
  implicit lazy val parser: Parser[SuppressWarningOptions] = Parser.derive
  implicit lazy val help: Help[SuppressWarningOptions]     = Help.derive
}
