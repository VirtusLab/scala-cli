package scala.cli.launcher

import caseapp.*

import scala.cli.commands.shared.HelpGroup
import scala.cli.commands.{Constants, tags}

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
  @HelpMessage(
    s"The default version of Scala used when processing user inputs (current default: ${Constants.defaultScalaVersion}). Can be overridden with --scala-version. "
  )
  @ValueDescription("version")
  @Hidden
  @Tag(tags.implementation)
  @Name("cliDefaultScalaVersion")
  cliUserScalaVersion: Option[String] = None,
  @Group(HelpGroup.Launcher.toString)
  @HelpMessage("")
  @Hidden
  @Tag(tags.implementation)
  @Name("r")
  @Name("repo")
  @Name("repository")
  @Name("predefinedRepository")
  cliPredefinedRepository: List[String] = Nil,
  @Group(HelpGroup.Launcher.toString)
  @HelpMessage("This allows to override the program name identified by Scala CLI as itself (the default is 'scala-cli')")
  @Hidden
  @Tag(tags.implementation)
  progName: Option[String] = None,
  @Recurse
  powerOptions: PowerOptions = PowerOptions()
)

object LauncherOptions {
  implicit lazy val parser: Parser[LauncherOptions] = Parser.derive
  implicit lazy val help: Help[LauncherOptions]     = Help.derive
}
