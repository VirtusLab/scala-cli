package scala.cli.commands.version

import caseapp.*

import scala.cli.ScalaCli.fullRunnerName
import scala.cli.commands.shared.{HasLoggingOptions, HelpGroup, HelpMessages, LoggingOptions}
import scala.cli.commands.tags
import scala.cli.signing.shared.PasswordOption
import scala.cli.signing.util.ArgParsers.*

// format: off
@HelpMessage(VersionOptions.helpMessage, "", VersionOptions.detailedHelpMessage)
final case class VersionOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Tag(tags.implementation)
  @Tag(tags.inShortHelp)
  @Group(HelpGroup.Version.toString)
  @HelpMessage(s"Show plain $fullRunnerName version only")
  @Name("cli")
    cliVersion: Boolean = false,
  @Group(HelpGroup.Version.toString)
  @HelpMessage("Show plain Scala version only")
  @Tag(tags.implementation)
  @Tag(tags.inShortHelp)
  @Name("scala")
    scalaVersion: Boolean = false,
  @Hidden
  @HelpMessage(HelpMessages.passwordOption)
  @Tag(tags.implementation)
    ghToken: Option[PasswordOption] = None,
  @Group(HelpGroup.Version.toString)
  @Tag(tags.implementation)
  @Tag(tags.inShortHelp)
  @HelpMessage(s"Don't check for the newest available $fullRunnerName version upstream")
    offline: Boolean = false
) extends HasLoggingOptions
// format: on

object VersionOptions {
  implicit lazy val parser: Parser[VersionOptions] = Parser.derive
  implicit lazy val help: Help[VersionOptions]     = Help.derive

  val cmdName = "version"
  private val helpHeader =
    s"Prints the version of the $fullRunnerName and the default version of Scala."
  val helpMessage: String = HelpMessages.shortHelpMessage(cmdName, helpHeader)
  val detailedHelpMessage: String =
    s"""$helpHeader (which can be overridden in the project)
       |If network connection is available, this sub-command also checks if the installed $fullRunnerName is up-to-date.
       |
       |The version of the $fullRunnerName is the version of the command-line tool that runs Scala programs, which
       |is distinct from the Scala version of the compiler. We recommend to specify the version of the Scala compiler
       |for a project in its sources (via a using directive). Otherwise, $fullRunnerName falls back to the default
       |Scala version defined by the runner.
       |
       |${HelpMessages.commandDocWebsiteReference(cmdName)}""".stripMargin
}
