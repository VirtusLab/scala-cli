package scala.cli.commands

import caseapp.*

// format: off
final case class SharedInputOptions(
  @Hidden
    defaultForbiddenDirectories: Boolean = true,
  @Hidden
    forbid: List[String] = Nil
)
// format: on

object SharedInputOptions {
  implicit lazy val parser: Parser[SharedInputOptions] = Parser.derive
  implicit lazy val help: Help[SharedInputOptions]     = Help.derive
}
