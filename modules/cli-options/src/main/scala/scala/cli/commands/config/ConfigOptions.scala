package scala.cli.commands.config

import caseapp._

import scala.cli.commands.{
  CoursierOptions,
  LoggingOptions,
  SharedDirectoriesOptions,
  SharedJvmOptions
}

// format: off
final case class ConfigOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    directories: SharedDirectoriesOptions = SharedDirectoriesOptions(),
  @Recurse
    coursier: CoursierOptions = CoursierOptions(),
  @Recurse
    jvm: SharedJvmOptions = SharedJvmOptions(),

  @Group("Config")
  @HelpMessage("Dump config DB as JSON")
  @Hidden
    dump: Boolean = false,
  @Group("Config")
  @HelpMessage("Create PGP key in config")
    createPgpKey: Boolean = false,
  @Group("Config")
  @HelpMessage("Email to use to create PGP key in config")
    email: Option[String] = None,
  @Group("Config")
  @HelpMessage("If the entry is a password, print the password value rather than how to get the password")
    password: Boolean = false,
  @Group("Config")
  @HelpMessage("If the entry is a password, save the password value rather than how to get the password")
    passwordValue: Boolean = false,
  @Group("Config")
  @HelpMessage("Remove an entry from config")
    unset: Boolean = false
)
// format: on

object ConfigOptions {
  implicit lazy val parser: Parser[ConfigOptions] = Parser.derive
  implicit lazy val help: Help[ConfigOptions]     = Help.derive
}
