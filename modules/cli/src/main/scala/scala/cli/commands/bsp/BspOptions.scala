package scala.cli.commands.bsp

import caseapp.*

import scala.cli.ScalaCli.fullRunnerName
import scala.cli.commands.shared.{GlobalOptions, HasGlobalOptions, HasSharedOptions, HelpMessages, SharedOptions}
import scala.cli.commands.tags

@HelpMessage(BspOptions.helpMessage, "", BspOptions.detailedHelpMessage)
final case class BspOptions(
  @Recurse
  @Hidden
    global: GlobalOptions = GlobalOptions(),
  @HelpMessage("Command-line options JSON file")
  @ValueDescription("path")
  @Hidden
  @Tag(tags.implementation)
    jsonOptions: Option[String] = None
) extends HasGlobalOptions

object BspOptions {
  implicit lazy val parser: Parser[BspOptions] = Parser.derive
  implicit lazy val help: Help[BspOptions]     = Help.derive
  val cmdName                                  = "bsp"
  private val helpHeader                       = "Start BSP server."
  val helpMessage: String =
    s"""$helpHeader
       |
       |${HelpMessages.commandFullHelpReference(cmdName)}
       |${HelpMessages.docsWebsiteReference}""".stripMargin
  val detailedHelpMessage: String =
    s"""$helpHeader
       |
       |BSP stands for Build Server Protocol.
       |For more information refer to https://build-server-protocol.github.io/
       |
       |This sub-command is not designed to be used by a human.
       |It is normally supposed to be invoked by your IDE when a $fullRunnerName project is imported.
       |
       |${HelpMessages.docsWebsiteReference}""".stripMargin
}
