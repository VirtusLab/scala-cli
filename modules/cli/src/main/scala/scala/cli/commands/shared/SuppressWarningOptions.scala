package scala.cli.commands.shared

import caseapp.*

import scala.cli.commands.tags

// format: off
final case class SuppressWarningOptions(
  @Tag(tags.implementation)
  @HelpMessage("Suppress warnings about using directives in multiple files")
  @Name("suppressWarningDirectivesInMultipleFiles")
    suppressDirectivesInMultipleFilesWarning: Option[Boolean] = None
)
// format: on

object SuppressWarningOptions {
  implicit lazy val parser: Parser[SuppressWarningOptions] = Parser.derive
  implicit lazy val help: Help[SuppressWarningOptions]     = Help.derive
}
