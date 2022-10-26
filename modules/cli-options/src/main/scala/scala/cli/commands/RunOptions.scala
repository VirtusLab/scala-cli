package scala.cli.commands

import caseapp.*
import caseapp.core.help.Help

import scala.cli.commands.common.HasSharedOptions

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
) extends HasSharedOptions
// format: on

object RunOptions {
  implicit lazy val parser: Parser[RunOptions] = Parser.derive
  implicit lazy val help: Help[RunOptions]     = Help.derive
}
