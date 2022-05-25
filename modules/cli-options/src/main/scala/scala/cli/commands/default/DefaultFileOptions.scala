package scala.cli.commands.default

import caseapp._

import scala.cli.commands.LoggingOptions

// format: off
final case class DefaultFileOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Group("Default")
  @HelpMessage("Write result to files rather than to stdout")
    write: Boolean = false,
  @Group("Default")
  @HelpMessage("List available default files")
    list: Boolean = false,
  @Group("Default")
  @HelpMessage("List available default file ids")
    listIds: Boolean = false,
  @Group("Default")
  @HelpMessage("Force overwriting destination files")
  @ExtraName("f")
    force: Boolean = false
)
// format: on

object DefaultFileOptions {
  implicit lazy val parser: Parser[DefaultFileOptions] = Parser.derive
  implicit lazy val help: Help[DefaultFileOptions]     = Help.derive
}
