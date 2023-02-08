package scala.cli.commands.installcompletions

import caseapp.*

import scala.cli.commands.shared.{HasLoggingOptions, LoggingOptions}
import scala.cli.commands.tags

// format: off
@HelpMessage("Installs completions into your shell")
final case class InstallCompletionsOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Name("shell")
  @Tag(tags.implementation)
  @HelpMessage("Name of the shell, either zsh or bash")
    format: Option[String] = None,

  @Tag(tags.implementation)
  @HelpMessage("Path to `*rc` file, defaults to `.bashrc` or `.zshrc` depending on shell")
  rcFile: Option[String] = None,

  @Tag(tags.implementation)
  @HelpMessage("Completions output directory")
  @Name("o")
  output: Option[String] = None,

  @Hidden
  @Tag(tags.implementation)
  @HelpMessage("Custom banner in comment placed in rc file")
  banner: String = "{NAME} completions",

  @Hidden
  @Tag(tags.implementation)
  @HelpMessage("Custom completions name")
  name: Option[String] = None,

  @Tag(tags.implementation)
  @HelpMessage("Print completions to stdout")
    env: Boolean = false,
) extends HasLoggingOptions
// format: on

object InstallCompletionsOptions {
  implicit lazy val parser: Parser[InstallCompletionsOptions] = Parser.derive
  implicit lazy val help: Help[InstallCompletionsOptions]     = Help.derive
}
