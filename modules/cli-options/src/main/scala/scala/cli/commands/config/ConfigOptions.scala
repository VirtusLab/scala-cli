package scala.cli.commands.config

import caseapp.*

import scala.cli.commands.common.HasLoggingOptions
import scala.cli.commands.{
  CoursierOptions,
  LoggingOptions,
  SharedDirectoriesOptions,
  SharedJvmOptions,
  tags
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
  @Tag(tags.experimental)
  @Hidden
    dump: Boolean = false,
  @Group("Config")
  @HelpMessage("Create PGP key in config")
  @Tag(tags.experimental)
    createPgpKey: Boolean = false,
  @Group("Config")
  @HelpMessage("Email to use to create PGP key in config")
  @Tag(tags.experimental)
    email: Option[String] = None,
  @Group("Config")
  @HelpMessage("If the entry is a password, print the password value rather than how to get the password")
  @Tag(tags.experimental)
    password: Boolean = false,
  @Group("Config")
  @HelpMessage("If the entry is a password, save the password value rather than how to get the password")
  @Tag(tags.experimental)
    passwordValue: Boolean = false,
  @Group("Config")
  @HelpMessage("Remove an entry from config")
  @ExtraName("remove")
  @Tag(tags.experimental)
    unset: Boolean = false,
  @Group("Config")
  @HelpMessage("For repository.credentials and publish.credentials, whether these credentials should be HTTPS only (default: true)")
  @Tag(tags.experimental)
    httpsOnly: Option[Boolean] = None,
  @Group("Config")
  @HelpMessage("For repository.credentials, whether to use these credentials automatically based on the host")
  @Tag(tags.experimental)
    matchHost: Option[Boolean] = None,
  @Group("Config")
  @HelpMessage("For repository.credentials, whether to use these credentials are optional")
  @Tag(tags.experimental)
    optional: Option[Boolean] = None,
  @Group("Config")
  @HelpMessage("For repository.credentials, whether to use these credentials should be passed upon redirection")
  @Tag(tags.experimental)
    passOnRedirect: Option[Boolean] = None
) extends HasLoggingOptions
// format: on

object ConfigOptions {
  implicit lazy val parser: Parser[ConfigOptions] = Parser.derive
  implicit lazy val help: Help[ConfigOptions]     = Help.derive
}
