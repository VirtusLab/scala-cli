package scala.cli.launcher

import caseapp.*

import scala.cli.commands.shared.HelpGroup
import scala.cli.commands.{Constants, tags}

case class ScalaRunnerLauncherOptions(
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
  @HelpMessage(
    "This allows to override the program name identified by Scala CLI as itself (the default is 'scala-cli')"
  )
  @Hidden
  @Tag(tags.implementation)
  progName: Option[String] = None,
  @Group(HelpGroup.Launcher.toString)
  @HelpMessage(
    "This allows to skip checking for newest Scala CLI versions. --offline covers this scenario as well."
  )
  @Hidden
  @Tag(tags.implementation)
  skipCliUpdates: Option[Boolean] = None,
  @Hidden
  @Tag(tags.implementation)
  predefinedCliVersion: Option[String] = None,
  @Hidden
  @Tag(tags.implementation)
  @Name("initialLauncher")
  initialLauncherPath: Option[String] = None
) {
  def toCliArgs: List[String] =
    cliUserScalaVersion.toList.flatMap(v => List("--cli-default-scala-version", v)) ++
      cliPredefinedRepository.flatMap(v => List("--repository", v)) ++
      progName.toList.flatMap(v => List("--prog-name", v)) ++
      skipCliUpdates.toList.filter(v => v).map(_ => "--skip-cli-updates") ++
      predefinedCliVersion.toList.flatMap(v => List("--predefined-cli-version", v)) ++
      initialLauncherPath.toList.flatMap(v => List("--initial-launcher-path", v))
}

object ScalaRunnerLauncherOptions {
  implicit val parser: Parser[ScalaRunnerLauncherOptions] = Parser.derive
  implicit val help: Help[ScalaRunnerLauncherOptions]     = Help.derive
}
