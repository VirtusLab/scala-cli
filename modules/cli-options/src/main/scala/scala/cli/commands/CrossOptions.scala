package scala.cli.commands

import caseapp._

// format: off
final case class CrossOptions(
    cross: Option[Boolean] = None
)
// format: on

object CrossOptions {
  lazy val parser: Parser[CrossOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[CrossOptions, parser.D] = parser
  implicit lazy val help: Help[CrossOptions]                      = Help.derive
}
