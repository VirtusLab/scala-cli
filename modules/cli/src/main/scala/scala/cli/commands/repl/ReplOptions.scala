package scala.cli.commands.repl

import caseapp.*

import scala.cli.ScalaCli.fullRunnerName
import scala.cli.commands.shared.{HasSharedOptions, HelpMessages, SharedOptions}

// format: off
@HelpMessage(
  s"""Fire-up a Scala REPL.
     |
     |The entire $fullRunnerName project's classpath is loaded to the repl.
     |
     |${HelpMessages.commandConfigurations("repl")}
     |
     |${HelpMessages.acceptedInputs}
     |
     |${HelpMessages.docsWebsiteReference}""".stripMargin)
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
}
