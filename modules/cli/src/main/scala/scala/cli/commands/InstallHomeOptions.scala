package scala.cli.commands

import caseapp._

// format: off
@HelpMessage(
  """Linking scala-cli binary to home
    |
    |Supported system: MacOS and Linux
    |""".stripMargin)
final case class InstallHomeOptions(        
  scalaCliBinaryPath: String
)
// format: on

object InstallHomeOptions {
  implicit val parser = Parser[InstallHomeOptions]
  implicit val help   = Help[InstallHomeOptions]
}
