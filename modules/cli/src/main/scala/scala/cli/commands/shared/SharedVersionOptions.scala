package scala.cli.commands.shared

import caseapp._

import scala.cli.commands.tags

// format: off
final case class SharedVersionOptions(
  @Group(HelpGroup.ProjectVersion.toString)
  @HelpMessage("Method used to compute the project version")
  @ValueDescription("git|git:tag|command:...")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    computeVersion: Option[String] = None,
  @Group(HelpGroup.ProjectVersion.toString)
  @HelpMessage("Set the project version")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    projectVersion: Option[String] = None
)
// format: on

object SharedVersionOptions {
  implicit lazy val parser: Parser[SharedVersionOptions] = Parser.derive
  implicit lazy val help: Help[SharedVersionOptions]     = Help.derive
}
