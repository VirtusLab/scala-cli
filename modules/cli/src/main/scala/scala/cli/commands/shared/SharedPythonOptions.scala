package scala.cli.commands.shared

import caseapp.*

import scala.cli.commands.{Constants, tags}

// format: off
final case class SharedPythonOptions(
  @Tag(tags.experimental)
  @HelpMessage("Set Java options so that Python can be loaded")
    pythonSetup: Option[Boolean] = None,
  @Tag(tags.experimental)
  @HelpMessage("Enable Python support via ScalaPy")
  @ExtraName("py")
    python: Option[Boolean] = None,
  @Tag(tags.experimental)
  @HelpMessage(s"Set ScalaPy version (${Constants.scalaPyVersion} by default)")
  @ExtraName("scalapyVersion")
    scalaPyVersion: Option[String] = None
)
// format: on

object SharedPythonOptions {
  implicit lazy val parser: Parser[SharedPythonOptions] = Parser.derive
  implicit lazy val help: Help[SharedPythonOptions]     = Help.derive
}
