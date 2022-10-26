package scala.cli.launcher

import caseapp.*

@HelpMessage("Run another Scala CLI version")
final case class LauncherOptions(
  @Group("Launcher")
  @HelpMessage("Set the Scala CLI version")
  cliVersion: Option[String] = None,
  @Group("Launcher")
  @HelpMessage("The version of Scala on which Scala CLI was published")
  @ValueDescription("2.12|2.13|3")
  cliScalaVersion: Option[String] = None,
  @Group("Launcher")
  @HelpMessage("When called as 'scala', allow to use power commands too")
  @Hidden
  power: Boolean = false
)

object LauncherOptions {
  lazy val parser: Parser[LauncherOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[LauncherOptions, parser.D] = parser
  implicit lazy val help: Help[LauncherOptions]                      = Help.derive
}
