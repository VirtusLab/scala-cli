package scala.cli.commands

import caseapp.*

import scala.cli.commands.common.HasLoggingOptions

// format: off
final case class AddPathOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  title: String = ""
) extends HasLoggingOptions
// format: on

object AddPathOptions {
  implicit lazy val parser: Parser[AddPathOptions] = Parser.derive
  implicit lazy val help: Help[AddPathOptions]     = Help.derive
}
