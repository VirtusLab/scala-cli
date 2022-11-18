package scala.cli.commands.addpath

import caseapp.*

import scala.cli.commands.shared.{HasLoggingOptions, LoggingOptions}

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
