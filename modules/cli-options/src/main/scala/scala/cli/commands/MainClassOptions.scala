package scala.cli.commands

import caseapp._

// format: off
final case class MainClassOptions(
  @Group("Entrypoint")
  @HelpMessage("Specify which main class to run")
  @ValueDescription("main-class")
  @Name("M")
    mainClass: Option[String] = None
)
// format: on

object MainClassOptions {
  lazy val parser: Parser[MainClassOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[MainClassOptions, parser.D] = parser
  implicit lazy val help: Help[MainClassOptions]                      = Help.derive
}
