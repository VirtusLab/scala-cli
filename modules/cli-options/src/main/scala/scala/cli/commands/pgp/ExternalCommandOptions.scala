package scala.cli.commands.pgp

import caseapp._

// A dummy options for external command
final case class ExternalCommandOptions()

object ExternalCommandOptions {
  implicit lazy val parser: Parser[ExternalCommandOptions] = Parser.derive
  implicit lazy val help: Help[ExternalCommandOptions]     = Help.derive
}
