package scala.cli.commands

import caseapp._

// format: off
final case class SharedWatchOptions(

  @HelpMessage("Watch sources for changes")
  @Name("w")
    watch: Boolean = false

)
// format: on

object SharedWatchOptions {
  lazy val parser: Parser[SharedWatchOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[SharedWatchOptions, parser.D] = parser
  implicit lazy val help: Help[SharedWatchOptions]                      = Help.derive
}
