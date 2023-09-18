package scala.cli.launcher

import caseapp.*

import scala.cli.commands.shared.HelpGroup
import scala.cli.commands.tags

case class PowerOptions(
  @Group(HelpGroup.Launcher.toString)
  @HelpMessage("When called as 'scala', allow to use power commands too")
  @Tag(tags.must)
  power: Boolean = false
)

object PowerOptions {
  implicit val parser: Parser[PowerOptions] = Parser.derive
  implicit val help: Help[PowerOptions]     = Help.derive
}
