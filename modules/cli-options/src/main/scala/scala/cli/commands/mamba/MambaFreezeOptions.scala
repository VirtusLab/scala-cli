package scala.cli.commands.mamba

import caseapp._

import scala.cli.commands.{CoursierOptions, LoggingOptions}

// format: off
final case class MambaFreezeOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    coursier: CoursierOptions = CoursierOptions(),
  @Recurse
    mamba: SharedMambaOptions = SharedMambaOptions(),

  @ExtraName("p")
    prefix: String
)
// format: on

object MambaFreezeOptions {
  lazy val parser: Parser[MambaFreezeOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[MambaFreezeOptions, parser.D] = parser
  implicit lazy val help: Help[MambaFreezeOptions]                      = Help.derive
}
