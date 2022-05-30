package scala.cli.commands

import caseapp._

// format: off
final case class ScalacExtraOptions(
  @Group("Scala")
  @HelpMessage("Show help for scalac. This is an alias for --scalac-option -help")
    scalacHelp: Boolean = false
)
// format: on
