package scala.cli.commands.mamba

import caseapp._

// format: off
final case class SharedMambaOptions()
// format: on

object SharedMambaOptions {
  lazy val parser: Parser[SharedMambaOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[SharedMambaOptions, parser.D] = parser
  implicit lazy val help: Help[SharedMambaOptions]                      = Help.derive
}
