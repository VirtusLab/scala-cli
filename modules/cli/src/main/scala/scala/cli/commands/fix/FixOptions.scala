package scala.cli.commands.fix

import caseapp.*
import caseapp.core.help.Help

import scala.cli.ScalaCli
import scala.cli.commands.shared.{HasSharedOptions, HelpMessages, SharedOptions}
import scala.cli.commands.tags

// format: off
@HelpMessage(FixOptions.helpMessage, "", FixOptions.detailedHelpMessage)
final case class FixOptions(
  @Recurse
    shared: SharedOptions = SharedOptions()
) extends HasSharedOptions
// format: on

object FixOptions {
  implicit lazy val parser: Parser[FixOptions] = Parser.derive
  implicit lazy val help: Help[FixOptions]     = Help.derive

  val cmdName             = "fix"
  private val helpHeader  = "Perform fixes on a Scala CLI project."
  val helpMessage: String = HelpMessages.shortHelpMessage(cmdName, helpHeader)
  val detailedHelpMessage: String =
    s"""$helpHeader
       |
       |${HelpMessages.commandConfigurations(cmdName)}
       |
       |${HelpMessages.acceptedInputs}
       |
       |${HelpMessages.commandDocWebsiteReference(cmdName)}""".stripMargin
}
