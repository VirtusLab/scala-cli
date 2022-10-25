package scala.cli.commands

import caseapp._

import scala.cli.commands.common.HasSharedOptions

// format: off
@HelpMessage("Fire-up a Scala REPL")
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
