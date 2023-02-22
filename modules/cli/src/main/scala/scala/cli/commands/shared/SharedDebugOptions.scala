package scala.cli.commands.shared

import caseapp.*
import caseapp.core.help.Help

import scala.cli.commands.tags

// format: off
final case class SharedDebugOptions(
  @Group(HelpGroup.Debug.toString)
  @HelpMessage("Turn debugging on")
  @Tag(tags.should)
    debug: Boolean = false,
  @Group(HelpGroup.Debug.toString)
  @HelpMessage("Debug port (5005 by default)")
  @Tag(tags.should)
    debugPort: Option[String] = None,
  @Group(HelpGroup.Debug.toString)
  @Tag(tags.should)
  @HelpMessage("Debug mode (attach by default)")
  @ValueDescription("attach|a|listen|l")
    debugMode: Option[String] = None
)
// format: on

object SharedDebugOptions {
  implicit lazy val parser: Parser[SharedDebugOptions] = Parser.derive
  implicit lazy val help: Help[SharedDebugOptions]     = Help.derive
}
