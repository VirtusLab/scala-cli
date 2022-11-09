package scala.cli.commands.shared

import caseapp.*

import scala.cli.commands.tags

// format: off
final case class MarkdownOptions(
  @Group("Markdown")
  @Tag(tags.experimental)
  @HelpMessage("Enable markdown support.")
  @Name("md")
  @Name("markdown")
    enableMarkdown: Boolean = false // TODO: add a separate scope for Markdown and remove this option once it's stable
)
// format: on

object MarkdownOptions {
  implicit lazy val parser: Parser[MarkdownOptions] = Parser.derive
  implicit lazy val help: Help[MarkdownOptions]     = Help.derive
}
