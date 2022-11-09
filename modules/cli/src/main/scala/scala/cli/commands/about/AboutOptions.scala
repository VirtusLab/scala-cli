package scala.cli.commands.about

import caseapp.*

import scala.cli.commands.shared.{HasLoggingOptions, HelpMessages, LoggingOptions}
import scala.cli.commands.tags
import scala.cli.signing.shared.PasswordOption
import scala.cli.signing.util.ArgParsers.*

// format: off
@HelpMessage("Print details about this application")
final case class AboutOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Hidden
  @HelpMessage(HelpMessages.passwordOption)
  @Tag(tags.implementation)
    ghToken: Option[PasswordOption] = None
) extends HasLoggingOptions
// format: on

object AboutOptions {
  implicit lazy val parser: Parser[AboutOptions] = Parser.derive
  implicit lazy val help: Help[AboutOptions]     = Help.derive
}
