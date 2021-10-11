package scala.cli.commands

import caseapp._

final case class CrossOptions(
  cross: Option[Boolean] = None
)

object CrossOptions {
  lazy val parser: Parser[CrossOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[CrossOptions, parser.D] = parser
  implicit lazy val help: Help[CrossOptions]                      = Help.derive
}
