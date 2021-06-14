package scala.cli.commands

import caseapp._

final case class InstallCompletionsOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    directories: SharedDirectoriesOptions = SharedDirectoriesOptions(),

  format: String,

  rcFile: Option[String] = None,
  directory: Option[String] = None,
  banner: String = "{NAME} CLI completions",
  name: String = "scala"
)
