package scala.cli.commands.config

import caseapp.*

import scala.cli.ScalaCli.{fullRunnerName, progName}
import scala.cli.commands.pgp.PgpScalaSigningOptions
import scala.cli.commands.shared.{
  CoursierOptions,
  HasLoggingOptions,
  HelpMessages,
  LoggingOptions,
  SharedJvmOptions
}
import scala.cli.commands.tags

// format: off
@HelpMessage(
  s"""Configure global settings for $fullRunnerName.
     |
     |Syntax:
     |  $progName ${HelpMessages.PowerString}config key value
     |For example, to globally set the interactive mode:
     |  $progName ${HelpMessages.PowerString}config interactive true
     |
     |${HelpMessages.commandDocWebsiteReference("misc/config")}""".stripMargin)
final case class ConfigOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Recurse
    coursier: CoursierOptions = CoursierOptions(),
  @Recurse
    jvm: SharedJvmOptions = SharedJvmOptions(),
  @Recurse
  @Tag(tags.restricted)
    scalaSigning: PgpScalaSigningOptions = PgpScalaSigningOptions(),
  @Group("Config")
  @HelpMessage("Dump config DB as JSON")
  @Hidden
  @Tag(tags.implementation)
    dump: Boolean = false,
  @Group("Config")
  @HelpMessage("Create PGP key in config")
    createPgpKey: Boolean = false,
  @Group("Config")
  @HelpMessage("Email to use to create PGP key in config")
  @Tag(tags.restricted)
    email: Option[String] = None,
  @Group("Config")
  @HelpMessage("If the entry is a password, print the password value rather than how to get the password")
  @Tag(tags.restricted)
    password: Boolean = false,
  @Group("Config")
  @HelpMessage("If the entry is a password, save the password value rather than how to get the password")
  @Tag(tags.restricted)
    passwordValue: Boolean = false,
  @Group("Config")
  @HelpMessage("Remove an entry from config")
  @ExtraName("remove")
    unset: Boolean = false,
  @Group("Config")
  @HelpMessage("For repository.credentials and publish.credentials, whether these credentials should be HTTPS only (default: true)")
  @Tag(tags.restricted)
    httpsOnly: Option[Boolean] = None,
  @Group("Config")
  @HelpMessage("For repository.credentials, whether to use these credentials automatically based on the host")
  @Tag(tags.restricted)
    matchHost: Option[Boolean] = None,
  @Group("Config")
  @HelpMessage("For repository.credentials, whether to use these credentials are optional")
  @Tag(tags.restricted)
    optional: Option[Boolean] = None,
  @Group("Config")
  @HelpMessage("For repository.credentials, whether to use these credentials should be passed upon redirection")
  @Tag(tags.restricted)
    passOnRedirect: Option[Boolean] = None
) extends HasLoggingOptions
// format: on

object ConfigOptions {
  implicit lazy val parser: Parser[ConfigOptions] = Parser.derive
  implicit lazy val help: Help[ConfigOptions]     = Help.derive
}
