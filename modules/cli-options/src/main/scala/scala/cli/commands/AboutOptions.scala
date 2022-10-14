package scala.cli.commands

import caseapp._

import scala.cli.signing.shared.PasswordOption
import scala.cli.signing.util.ArgParsers._

// format: off
@HelpMessage("Print details about this application")
final case class AboutOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Hidden
  @HelpMessage(HelpMessages.passwordOption)
    ghToken: Option[PasswordOption] = None
)
// format: on


object AboutOptions {
  implicit lazy val parser: Parser[AboutOptions] = Parser.derive
  implicit lazy val help: Help[AboutOptions]   = Help.derive
}
