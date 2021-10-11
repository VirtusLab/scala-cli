package scala.cli.commands

import caseapp._

// format: off
@HelpMessage("Install scala-cli in a sub-directory of the home directory")
final case class InstallHomeOptions(
  @Group("InstallHome")
    scalaCliBinaryPath: String,
  @Group("InstallHome")
  @Name("f")
  @HelpMessage("Overwrite scala-cli if exists")
    force: Boolean = false,
)
// format: on

object InstallHomeOptions {
  implicit lazy val parser: Parser[InstallHomeOptions] = Parser.derive
  implicit lazy val help: Help[InstallHomeOptions]     = Help.derive
}
