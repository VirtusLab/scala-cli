package scala.cli.commands

import caseapp.*

// format: off
final case class CrossOptions(
    @HelpMessage("[experimental] Run given command against all provided Scala versions and/or platforms")
    cross: Option[Boolean] = None
)
// format: on

object CrossOptions {
  lazy val parser: Parser[CrossOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[CrossOptions, parser.D] = parser
  implicit lazy val help: Help[CrossOptions]                      = Help.derive
}
