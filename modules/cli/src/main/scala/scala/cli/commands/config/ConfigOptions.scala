package scala.cli.commands.config

import caseapp.*

import scala.build.internal.util.ConsoleUtils.ScalaCliConsole
import scala.cli.ScalaCli.{allowRestrictedFeatures, fullRunnerName, progName}
import scala.cli.commands.pgp.PgpScalaSigningOptions
import scala.cli.commands.shared.*
import scala.cli.commands.tags
import scala.cli.config.{Key, Keys}

// format: off
@HelpMessage(ConfigOptions.helpMessage, "", ConfigOptions.detailedHelpMessage)
final case class ConfigOptions(
  @Recurse
    global: GlobalOptions = GlobalOptions(),
  @Recurse
    coursier: CoursierOptions = CoursierOptions(),
  @Recurse
    jvm: SharedJvmOptions = SharedJvmOptions(),
  @Recurse
    scalaSigning: PgpScalaSigningOptions = PgpScalaSigningOptions(),
  @Group(HelpGroup.Config.toString)
  @HelpMessage("Dump config DB as JSON")
  @Hidden
  @Tag(tags.implementation)
  @Tag(tags.inShortHelp)
    dump: Boolean = false,
  @Group(HelpGroup.Config.toString)
  @HelpMessage("Create PGP key in config")
  @Tag(tags.experimental)
    createPgpKey: Boolean = false,
  @Group(HelpGroup.Config.toString)
  @HelpMessage("Email to use to create PGP key in config")
  @Tag(tags.experimental)
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
) extends HasGlobalOptions
// format: on

object ConfigOptions {
  implicit lazy val parser: Parser[ConfigOptions] = Parser.derive
  implicit lazy val help: Help[ConfigOptions]     = Help.derive
  private val helpHeader: String = s"Configure global settings for $fullRunnerName."
  private val cmdName            = "config"
  val helpMessage: String =
    s"""$helpHeader
       |
       |Available keys:
       |  ${configKeyMessages(includeHidden = false).mkString(s"${System.lineSeparator}  ")}
       |
       |${HelpMessages.commandFullHelpReference(cmdName)}
       |${HelpMessages.commandDocWebsiteReference(cmdName)}""".stripMargin
  private def configKeyMessages(includeHidden: Boolean): Seq[String] = {
    val allKeys: Seq[Key[_]] = Keys.map.values.toSeq
    val allowedKeys: Seq[Key[_]] =
      if allowRestrictedFeatures then allKeys
      else allKeys.filterNot(k => k.isRestricted || k.isExperimental)
    val keys: Seq[Key[_]] =
      if includeHidden then allowedKeys
      else allowedKeys.filterNot(k => k.hidden || k.isExperimental)
    val maxFullNameLength = keys.map(_.fullName.length).max
    keys.sortBy(_.fullName)
      .map { key =>
        val currentKeyFullNameLength = maxFullNameLength - key.fullName.length
        val extraSpaces =
          if currentKeyFullNameLength > 0 then " " * currentKeyFullNameLength else ""
        val hiddenOrExperimentalString =
          if key.hidden then s"${ScalaCliConsole.GRAY}(hidden)${Console.RESET} "
          else if key.isRestricted then s"${ScalaCliConsole.GRAY}(power)${Console.RESET} "
          else if key.isExperimental then s"${ScalaCliConsole.GRAY}(experimental)${Console.RESET} "
          else ""
        s"${Console.YELLOW}${key.fullName}${Console.RESET}$extraSpaces  $hiddenOrExperimentalString${key.description}"
      }
  }
  val detailedHelpMessage: String =
    s"""$helpHeader
       |
       |Syntax:
       |  ${Console.BOLD}$progName $cmdName key value${Console.RESET}
       |For example, to globally set the interactive mode:
       |  ${Console.BOLD}$progName $cmdName interactive true${Console.RESET}
       |  
       |Available keys:
       |  ${configKeyMessages(includeHidden = true).mkString(s"${System.lineSeparator}  ")}
       |
       |${HelpMessages.commandDocWebsiteReference(cmdName)}""".stripMargin
}
