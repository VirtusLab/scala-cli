package scala.cli.commands

import caseapp._

import scala.cli.commands.common.HasLoggingOptions
import scala.cli.signing.shared.PasswordOption
import scala.cli.signing.util.ArgParsers._

// format: off
@HelpMessage("Print details about this application")
final case class DoctorOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Hidden
  @HelpMessage(HelpMessages.passwordOption)
    ghToken: Option[PasswordOption] = None
) extends HasLoggingOptions
// format: on

object DoctorOptions {
  implicit lazy val parser: Parser[DoctorOptions] = Parser.derive
  implicit lazy val help: Help[DoctorOptions]     = Help.derive
}
