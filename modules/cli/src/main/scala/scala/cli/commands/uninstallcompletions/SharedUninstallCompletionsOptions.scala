package scala.cli.commands.uninstallcompletions

import caseapp.*

import scala.cli.commands.tags

// format: off
final case class SharedUninstallCompletionsOptions(
  @Group("Uninstall")
  @HelpMessage("Path to `*rc` file, defaults to `.bashrc` or `.zshrc` depending on shell")
  @Tag(tags.implementation)
  @Tag(tags.inShortHelp)
  rcFile: Option[String] = None,
  @Group("Uninstall")
  @Hidden
  @HelpMessage("Custom banner in comment placed in rc file")
  @Tag(tags.implementation)
  banner: String = "{NAME} completions",
  @Group("Uninstall")
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
