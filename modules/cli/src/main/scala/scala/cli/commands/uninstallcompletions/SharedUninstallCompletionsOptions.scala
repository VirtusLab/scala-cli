package scala.cli.commands.uninstallcompletions

import caseapp.*

import scala.cli.commands.shared.HelpGroup
import scala.cli.commands.tags

// format: off
final case class SharedUninstallCompletionsOptions(
  @Group(HelpGroup.Uninstall.toString)
  @HelpMessage("Path to `*rc` file, defaults to `.bashrc` or `.zshrc` depending on shell")
  @Tag(tags.implementation)
  @Tag(tags.inShortHelp)
  rcFile: Option[String] = None,
  @Group(HelpGroup.Uninstall.toString)
  @Hidden
  @HelpMessage("Custom banner in comment placed in rc file")
  @Tag(tags.implementation)
  banner: String = "{NAME} completions",
  @Group(HelpGroup.Uninstall.toString)
  @Hidden
  @HelpMessage("Custom completions name")
  @Tag(tags.implementation)
  name: Option[String] = None
)
// format: on

object SharedUninstallCompletionsOptions {
  implicit lazy val parser: Parser[SharedUninstallCompletionsOptions] = Parser.derive
  implicit lazy val help: Help[SharedUninstallCompletionsOptions]     = Help.derive
}
