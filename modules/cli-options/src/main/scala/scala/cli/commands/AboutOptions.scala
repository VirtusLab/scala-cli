package scala.cli.commands

import caseapp._

// format: off
@HelpMessage("Print details about this application")
final case class AboutOptions(
  @Recurse
    verbosity: VerbosityOptions = VerbosityOptions()
)
// format: on


object AboutOptions {
  implicit lazy val parser: Parser[AboutOptions] = Parser.derive
  implicit lazy val help: Help[AboutOptions]   = Help.derive
}
