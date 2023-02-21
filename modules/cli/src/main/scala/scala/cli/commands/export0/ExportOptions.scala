package scala.cli.commands.export0

import caseapp.*

import scala.cli.ScalaCli.fullRunnerName
import scala.cli.commands.shared.{HasSharedOptions, HelpMessages, MainClassOptions, SharedOptions}
import scala.cli.commands.tags

// format: off
@HelpMessage(ExportOptions.helpMessage, "", ExportOptions.detailedHelpMessage)
final case class ExportOptions(
  // FIXME There might be too many options for 'scala-cli export' there
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Recurse
    mainClass: MainClassOptions = MainClassOptions(),

  @Group("Build Tool export")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
  @HelpMessage("Sets the export format to SBT")
    sbt: Option[Boolean] = None,
  @Group("Build Tool export")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
  @HelpMessage("Sets the export format to Mill")
    mill: Option[Boolean] = None,
  @Tag(tags.restricted)
  @HelpMessage("Sets the export format to Json")
    json: Option[Boolean] = None,

  @Name("setting")
  @Group("Build Tool export")
  @Tag(tags.restricted)
    sbtSetting: List[String] = Nil,
  @Name("p")
  @Group("Build Tool export")
  @Tag(tags.restricted)
  @HelpMessage("Project name to be used on Mill build file")
    project: Option[String] = None,
  @Group("Build Tool export")
  @Tag(tags.restricted)
  @HelpMessage("Version of SBT to be used for the export")
    sbtVersion: Option[String] = None,
  @Name("o")
  @Group("Build Tool export")
  @Tag(tags.restricted)
    output: Option[String] = None
) extends HasSharedOptions
// format: on
object ExportOptions {
  implicit lazy val parser: Parser[ExportOptions] = Parser.derive
  implicit lazy val help: Help[ExportOptions]     = Help.derive

  private val helpHeader = "Export current project to an external build tool (like SBT or Mill)."
  val helpMessage: String =
    s"""$helpHeader
       |
       |${HelpMessages.docsWebsiteReference}""".stripMargin
  val detailedHelpMessage: String =
    s"""$helpHeader
       |
       |The whole $fullRunnerName project should get exported along with its dependencies configuration.
       |
       |Unless otherwise configured, the default export format is SBT.
       |
       |${HelpMessages.acceptedInputs}
       |
       |${HelpMessages.docsWebsiteReference}""".stripMargin
}
