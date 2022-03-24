package scala.cli.commands

import caseapp._

case class DefaultOptions(
  @Recurse
  runOptions: RunOptions = RunOptions(),
  @Name("-version")
  version: Boolean = false
)

object DefaultOptions {
  implicit lazy val parser: Parser[DefaultOptions] = Parser.derive
  implicit lazy val help: Help[DefaultOptions]     = Help.derive

}
