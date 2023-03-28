package scala.cli.commands.update

import caseapp.*

import scala.cli.ScalaCli.{baseRunnerName, fullRunnerName}
import scala.cli.commands.shared.{GlobalOptions, HasGlobalOptions, HelpGroup, HelpMessages}
import scala.cli.commands.tags
import scala.cli.signing.shared.PasswordOption
import scala.cli.signing.util.ArgParsers.*

// format: off
@HelpMessage(UpdateOptions.helpMessage, "", UpdateOptions.detailedHelpMessage)
final case class UpdateOptions(
  @Recurse
    global: GlobalOptions = GlobalOptions(),
  @Hidden
  @Group(HelpGroup.Update.toString)
  @HelpMessage("Binary name")
  @Tag(tags.implementation)
    binaryName: String = baseRunnerName,
  @Hidden
  @Group(HelpGroup.Update.toString)
  @HelpMessage("Binary directory")
  @Tag(tags.implementation)
    binDir: Option[String] = None,
  @Name("f")
  @Group(HelpGroup.Update.toString)
  @HelpMessage(s"Force update $fullRunnerName if it is outdated")
  @Tag(tags.implementation)
  @Tag(tags.inShortHelp)
    force: Boolean = false,
  @Hidden
  @Tag(tags.implementation)
    isInternalRun: Boolean = false,
  @Hidden
  @HelpMessage(HelpMessages.passwordOption)
  @Tag(tags.implementation)
    ghToken: Option[PasswordOption] = None
) extends HasGlobalOptions
// format: on

object UpdateOptions {
  implicit lazy val parser: Parser[UpdateOptions] = Parser.derive
  implicit lazy val help: Help[UpdateOptions]     = Help.derive

  private val helpHeader =
    s"""Updates $fullRunnerName.
       |Works only when installed with the installation script.
       |If $fullRunnerName was installed with an external tool, refer to its update methods.""".stripMargin
  val helpMessage: String =
    s"""$helpHeader
       |
       |${HelpMessages.commandFullHelpReference("update")}
       |${HelpMessages.installationDocsWebsiteReference}""".stripMargin
  val detailedHelpMessage: String =
    s"""$helpHeader
       |
       |${HelpMessages.installationDocsWebsiteReference}""".stripMargin
}
