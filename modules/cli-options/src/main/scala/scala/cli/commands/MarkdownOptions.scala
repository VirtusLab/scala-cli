package scala.cli.commands

import caseapp.*

// format: off
final case class MarkdownOptions(
  @Group("Markdown")
  @HelpMessage("[experimental] Enable markdown support.")
  @Name("md")
  @Name("markdown")
    enableMarkdown: Boolean = false // TODO: add a separate scope for Markdown and remove this option once it's stable
)
// format: on

object MarkdownOptions {
  lazy val parser: Parser[MarkdownOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[MarkdownOptions, parser.D] = parser
  implicit lazy val help: Help[MarkdownOptions]                      = Help.derive
}
