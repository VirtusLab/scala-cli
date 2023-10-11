package scala.cli.launcher

import caseapp.*

import scala.cli.commands.shared.HelpGroup
import scala.cli.commands.tags

/** Options extracted from [[LauncherOptions]] to allow for parsing them separately. Thanks to this
  * and additional parsing we can read the --power flag placed anywhere in the command invocation.
  *
  * This option is duplicated in [[scala.cli.commands.shared.GlobalOptions]] so that we can ensure
  * that no subcommand defines its own --power option Checking for clashing names is done in unit
  * tests.
  */
case class PowerOptions(
  @Group(HelpGroup.Launcher.toString)
  @HelpMessage("Allows to use restricted & experimental features")
  @Tag(tags.must)
  power: Boolean = false
)

object PowerOptions {
  implicit val parser: Parser[PowerOptions] = Parser.derive
  implicit val help: Help[PowerOptions]     = Help.derive
}
