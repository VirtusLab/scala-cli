package scala.cli.commands

import caseapp._

// format: off
final case class InstallCompletionsOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    directories: SharedDirectoriesOptions = SharedDirectoriesOptions(),

  @Name("shell")
    format: Option[String] = None,

  rcFile: Option[String] = None,
  directory: Option[String] = None,
  banner: String = "{NAME} completions",
  name: Option[String] = None,
  env: Boolean = false
)
// format: on

object InstallCompletionsOptions {
  implicit val parser = Parser[InstallCompletionsOptions]
  implicit val help   = Help[InstallCompletionsOptions]
}
