package scala.cli.commands.run

import caseapp.*
import caseapp.core.help.Help

import scala.cli.ScalaCli
import scala.cli.commands.shared.{HasSharedOptions, HelpMessages, SharedOptions}

@HelpMessage(RunOptions.helpMessage, "", RunOptions.detailedHelpMessage)
// format: off
final case class RunOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Recurse
    sharedRun: SharedRunOptions = SharedRunOptions()
) extends HasSharedOptions
// format: on

object RunOptions {
  implicit lazy val parser: Parser[RunOptions] = Parser.derive
  implicit lazy val help: Help[RunOptions]     = Help.derive

  val cmdName            = "run"
  private val helpHeader = "Compile and run Scala code."
  val helpMessage: String =
    s"""$helpHeader
       |
       |${HelpMessages.commandFullHelpReference(cmdName)}
       |${HelpMessages.commandDocWebsiteReference(cmdName)}""".stripMargin
  val detailedHelpMessage: String =
    s"""$helpHeader
       |
       |${HelpMessages.commandConfigurations(cmdName)}
       |
       |For a run to be successful, a main method must be present on the classpath.
       |.sc scripts are an exception, as a main class is provided in their wrapper.
       |
       |${HelpMessages.acceptedInputs}
       |
       |To pass arguments to the actual application, just add them after `--`, like:
       |
       |```sh
       |${ScalaCli.progName} run Main.scala AnotherSource.scala -- first-arg second-arg
       |```
       |
       |${HelpMessages.commandDocWebsiteReference(cmdName)}""".stripMargin
}
