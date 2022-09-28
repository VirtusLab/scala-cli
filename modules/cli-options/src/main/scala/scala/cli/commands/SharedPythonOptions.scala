package scala.cli.commands

import caseapp._

// format: off
final case class SharedPythonOptions(
  @HelpMessage("[experimental] Set Java options so that Python can be loaded")
    pythonSetup: Option[Boolean] = None,
  @HelpMessage("[experimental] Enable Python support via ScalaPy")
  @ExtraName("py")
    python: Option[Boolean] = None,
  @HelpMessage("[experimental] Set ScalaPy version")
  @ExtraName("scalapyVersion")
    scalaPyVersion: Option[String] = None
)
// format: on

object SharedPythonOptions {
  lazy val parser: Parser[SharedPythonOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[SharedPythonOptions, parser.D] = parser
  implicit lazy val help: Help[SharedPythonOptions]                      = Help.derive
}
