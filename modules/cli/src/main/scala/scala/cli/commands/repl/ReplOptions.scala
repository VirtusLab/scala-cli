package scala.cli.commands.repl

import caseapp.*

import scala.cli.ScalaCli.fullRunnerName
import scala.cli.commands.shared.{HasSharedOptions, HelpMessages, SharedOptions}

@HelpMessage(ReplOptions.helpMessage, "", ReplOptions.detailedHelpMessage)
// format: off
final case class ReplOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Recurse
    sharedRepl: SharedReplOptions = SharedReplOptions()
) extends HasSharedOptions
// format: on

object ReplOptions {
  implicit lazy val parser: Parser[ReplOptions] = Parser.derive
  implicit lazy val help: Help[ReplOptions]     = Help.derive
  val cmdName                                   = "repl"
  private val helpHeader                        = "Fire-up a Scala REPL."
  val helpMessage: String                       = HelpMessages.shortHelpMessage(cmdName, helpHeader)
  val detailedHelpMessage: String =
    s"""$helpHeader
       |
       |The entire $fullRunnerName project's classpath is loaded to the repl.
       |
       |${HelpMessages.commandConfigurations(cmdName)}
       |
       |${HelpMessages.acceptedInputs}
       |
       |${HelpMessages.commandDocWebsiteReference(cmdName)}""".stripMargin
}
