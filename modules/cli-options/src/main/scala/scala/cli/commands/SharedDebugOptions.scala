package scala.cli.commands

import caseapp.*
import caseapp.core.help.Help

import scala.cli.commands.BloopExitOptions.parser

// format: off
final case class SharedDebugOptions(
  @Group("Debug")
  @HelpMessage("Turn debugging on")
    debug: Boolean = false,
  @Group("Debug")
  @HelpMessage("Debug port (5005 by default)")
    debugPort: Option[String] = None,
  @Group("Debug")
  @HelpMessage("Debug mode (attach by default)")
  @ValueDescription("attach|a|listen|l")
    debugMode: Option[String] = None
)
// format: on

object SharedDebugOptions {
  implicit lazy val parser: Parser[SharedDebugOptions] = Parser.derive
  implicit lazy val help: Help[SharedDebugOptions]     = Help.derive
  // Parser.Aux for using SharedDebugOptions with @Recurse in other options
  implicit lazy val parserAux: Parser.Aux[SharedDebugOptions, parser.D] = parser
}
