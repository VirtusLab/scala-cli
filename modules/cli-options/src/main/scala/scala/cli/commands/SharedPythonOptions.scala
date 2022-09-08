package scala.cli.commands

import caseapp._

// format: off
final case class SharedPythonOptions(
  @HelpMessage("Set Java options so that Python can be loaded")
    pythonSetup: Option[Boolean] = None,
  @HelpMessage("Enable Python support via ScalaPy")
  @ExtraName("py")
    python: Option[Boolean] = None
)
// format: on

object SharedPythonOptions {
  lazy val parser: Parser[SharedPythonOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[SharedPythonOptions, parser.D] = parser
  implicit lazy val help: Help[SharedPythonOptions]                      = Help.derive
}
