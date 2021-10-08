package scala.cli.commands

import caseapp._

// format: off
final case class CompileCrossOptions(
  @HelpMessage("Cross-compile sources")
    cross: Option[Boolean] = None
)
// format: on

object CompileCrossOptions {
  lazy val parser: Parser[CompileCrossOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[CompileCrossOptions, parser.D] = parser
  implicit lazy val help: Help[CompileCrossOptions]                      = Help.derive
}
