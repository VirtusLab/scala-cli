package scala.cli.commands

import caseapp.*

// format: off
final case class SharedInputOptions(
  @Hidden
  @Tag(tags.implementation)
    defaultForbiddenDirectories: Boolean = true,
  @Hidden
  @Tag(tags.implementation)
    forbid: List[String] = Nil
)
// format: on

object SharedInputOptions {
  lazy val parser: Parser[SharedInputOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[SharedInputOptions, parser.D] = parser
  implicit lazy val help: Help[SharedInputOptions]                      = Help.derive
}
