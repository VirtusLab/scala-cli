package scala.cli.commands

import caseapp._

// format: off
@HelpMessage("Install scala-cli in a sub-directory of the home directory")
final case class InstallHomeOptions(        
  scalaCliBinaryPath: String
)
// format: on

object InstallHomeOptions {
  implicit val parser = Parser[InstallHomeOptions]
  implicit val help   = Help[InstallHomeOptions]
}
