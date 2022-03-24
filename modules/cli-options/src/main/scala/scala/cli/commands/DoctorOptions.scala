package scala.cli.commands

import caseapp._

// format: off
@HelpMessage("Print details about this application")
final case class DoctorOptions(
  @Recurse
    verbosity: VerbosityOptions = VerbosityOptions()
)
// format: on

object DoctorOptions {
  implicit lazy val healtCheck: Parser[DoctorOptions] = Parser.derive
  implicit lazy val help: Help[DoctorOptions]         = Help.derive
}
