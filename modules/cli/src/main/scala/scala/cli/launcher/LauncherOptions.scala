package scala.cli.launcher

import caseapp.*

import scala.cli.commands.shared.HelpGroup
import scala.cli.commands.tags

@HelpMessage("Run another Scala CLI version")
final case class LauncherOptions(
  @Group(HelpGroup.Launcher.toString)
  @HelpMessage("Set the Scala CLI version")
  @ValueDescription("nightly|version")
  @Tag(tags.implementation)
  @Tag(tags.inShortHelp)
  cliVersion: Option[String] = None,
  @Group(HelpGroup.Launcher.toString)
  @HelpMessage("The version of Scala on which Scala CLI was published")
  @ValueDescription("2.12|2.13|3")
  @Hidden
  @Tag(tags.implementation)
  cliScalaVersion: Option[String] = None,
  @Group(HelpGroup.Launcher.toString)
  @HelpMessage("When called as 'scala', allow to use power commands too")
  @Tag(tags.must)
  power: Boolean = false
)

object LauncherOptions {
  implicit lazy val parser: Parser[LauncherOptions] = Parser.derive
  implicit lazy val help: Help[LauncherOptions]     = Help.derive
}
