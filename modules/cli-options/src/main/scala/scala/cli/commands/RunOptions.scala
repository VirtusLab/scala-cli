package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

// format: off
@HelpMessage("""|Compile and run Scala code.
                |
                |To pass arguments to the application, just add them after `--`, like:
                |
                |```sh
                |scala-cli MyApp.scala -- first-arg second-arg
                |```""".stripMargin)
final case class RunOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Recurse
    sharedRun: SharedRunOptions = SharedRunOptions()
)
// format: on

object RunOptions {
  implicit lazy val parser: Parser[RunOptions] = Parser.derive
  implicit lazy val help: Help[RunOptions]     = Help.derive
}
