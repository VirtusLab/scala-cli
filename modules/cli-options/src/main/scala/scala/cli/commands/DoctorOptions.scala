package scala.cli.commands

import caseapp.*

import scala.cli.commands.common.HasLoggingOptions
import scala.cli.signing.shared.PasswordOption
import scala.cli.signing.util.ArgParsers.*

// format: off
@HelpMessage("Print details about this application")
final case class DoctorOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @HelpMessage(HelpMessages.passwordOption)
  @Tag(tags.implementation)
    ghToken: Option[PasswordOption] = None
) extends HasLoggingOptions
// format: on

object DoctorOptions {
  implicit lazy val parser: Parser[DoctorOptions] = Parser.derive
  implicit lazy val help: Help[DoctorOptions]     = Help.derive
}
