package scala.cli.commands

import caseapp._

// format: off
@HelpMessage("Installs completions into your shell")
final case class InstallCompletionsOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    directories: SharedDirectoriesOptions = SharedDirectoriesOptions(),

  @Name("shell")
  @HelpMessage("Name of the shell, either zsh or bash")
    format: Option[String] = None,

  @HelpMessage("Path to *rc file, defaults to .bashrc or .zshrc depending on shell")
  rcFile: Option[String] = None,

  @HelpMessage("Completions output directory")
  @Name("o")
  output: Option[String] = None,

  @Hidden
  @HelpMessage("Custom banner in comment placed in rc file")
  banner: String = "{NAME} completions",

  @Hidden
  @HelpMessage("Custom completions name")
  name: Option[String] = None,

  @HelpMessage("Print completions to stdout")
  env: Boolean = false
)
// format: on

object InstallCompletionsOptions {
  implicit lazy val parser: Parser[InstallCompletionsOptions] = Parser.derive
  implicit lazy val help: Help[InstallCompletionsOptions]     = Help.derive
}
