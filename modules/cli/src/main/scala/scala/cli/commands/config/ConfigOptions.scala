package scala.cli.commands.config

import caseapp.*

import scala.cli.ScalaCli.{fullRunnerName, progName}
import scala.cli.commands.pgp.PgpScalaSigningOptions
import scala.cli.commands.shared.{
  CoursierOptions,
  HasLoggingOptions,
  HelpGroup,
  HelpMessages,
  LoggingOptions,
  SharedJvmOptions
}
import scala.cli.commands.tags

// format: off
@HelpMessage(ConfigOptions.helpMessage, "", ConfigOptions.detailedHelpMessage)
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
  @Group(HelpGroup.Config.toString)
  @HelpMessage("Dump config DB as JSON")
  @Hidden
  @Tag(tags.implementation)
  @Tag(tags.inShortHelp)
    dump: Boolean = false,
  @Group(HelpGroup.Config.toString)
  @HelpMessage("Create PGP key in config")
  @Tag(tags.inShortHelp)
  @Tag(tags.restricted)
    createPgpKey: Boolean = false,
  @Group(HelpGroup.Config.toString)
  @HelpMessage("Email to use to create PGP key in config")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    email: Option[String] = None,
  @Group(HelpGroup.Config.toString)
  @HelpMessage("If the entry is a password, print the password value rather than how to get the password")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    password: Boolean = false,
  @Group(HelpGroup.Config.toString)
  @HelpMessage("If the entry is a password, save the password value rather than how to get the password")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    passwordValue: Boolean = false,
  @Group(HelpGroup.Config.toString)
  @HelpMessage("Remove an entry from config")
  @Tag(tags.inShortHelp)
  @Tag(tags.should)
  @ExtraName("remove")
    unset: Boolean = false,
  @Group(HelpGroup.Config.toString)
  @HelpMessage("For repository.credentials and publish.credentials, whether these credentials should be HTTPS only (default: true)")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    httpsOnly: Option[Boolean] = None,
  @Group(HelpGroup.Config.toString)
  @HelpMessage("For repository.credentials, whether to use these credentials automatically based on the host")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    matchHost: Option[Boolean] = None,
  @Group(HelpGroup.Config.toString)
  @HelpMessage("For repository.credentials, whether to use these credentials are optional")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    optional: Option[Boolean] = None,
  @Group(HelpGroup.Config.toString)
  @HelpMessage("For repository.credentials, whether to use these credentials should be passed upon redirection")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    passOnRedirect: Option[Boolean] = None
) extends HasLoggingOptions
// format: on

object ConfigOptions {
  implicit lazy val parser: Parser[ConfigOptions] = Parser.derive
  implicit lazy val help: Help[ConfigOptions]     = Help.derive
  private val helpHeader: String = s"Configure global settings for $fullRunnerName."
  private val cmdName            = "config"
  private val websiteSuffix      = s"misc/$cmdName"
  val helpMessage: String =
    s"""$helpHeader
       |
       |${HelpMessages.commandFullHelpReference(cmdName)}
       |${HelpMessages.commandDocWebsiteReference(websiteSuffix)}""".stripMargin
  val detailedHelpMessage: String =
    s"""$helpHeader
       |
       |Syntax:
       |  $progName $cmdName key value
       |For example, to globally set the interactive mode:
       |  $progName $cmdName interactive true
       |
       |${HelpMessages.commandDocWebsiteReference(websiteSuffix)}""".stripMargin
}
