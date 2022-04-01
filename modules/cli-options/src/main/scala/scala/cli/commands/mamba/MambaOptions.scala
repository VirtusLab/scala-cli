package scala.cli.commands.mamba

import caseapp._

import scala.cli.commands.{CoursierOptions, LoggingOptions}

// format: off
@HelpMessage("Run mamba (lightweight native mamba executable)")
final case class MambaOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    coursier: CoursierOptions = CoursierOptions(),
  @Recurse
    mamba: SharedMambaOptions = SharedMambaOptions()
)
// format: on

object MambaOptions {
  lazy val parser: Parser[MambaOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[MambaOptions, parser.D] = parser
  implicit lazy val help: Help[MambaOptions]                      = Help.derive
}
