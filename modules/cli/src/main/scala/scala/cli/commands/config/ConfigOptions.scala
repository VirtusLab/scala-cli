package scala.cli.commands.config

import caseapp.*

import scala.cli.commands.pgp.PgpScalaSigningOptions
import scala.cli.commands.shared.{
  CoursierOptions,
  HasLoggingOptions,
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
  @Recurse
    scalaSigning: PgpScalaSigningOptions = PgpScalaSigningOptions(),
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
  @ExtraName("remove")
    unset: Boolean = false,
  @Group("Config")
  @HelpMessage("For repository.credentials and publish.credentials, whether these credentials should be HTTPS only (default: true)")
    httpsOnly: Option[Boolean] = None,
  @Group("Config")
  @HelpMessage("For repository.credentials, whether to use these credentials automatically based on the host")
    matchHost: Option[Boolean] = None,
  @Group("Config")
  @HelpMessage("For repository.credentials, whether to use these credentials are optional")
    optional: Option[Boolean] = None,
  @Group("Config")
  @HelpMessage("For repository.credentials, whether to use these credentials should be passed upon redirection")
    passOnRedirect: Option[Boolean] = None
) extends HasLoggingOptions
// format: on

object ConfigOptions {
  implicit lazy val parser: Parser[ConfigOptions] = Parser.derive
  implicit lazy val help: Help[ConfigOptions]     = Help.derive
}
