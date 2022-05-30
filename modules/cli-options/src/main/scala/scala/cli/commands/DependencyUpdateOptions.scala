package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

// format: off
@HelpMessage("Update dependencies in project")
final case class DependencyUpdateOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Group("DependencyUpdate")
  @HelpMessage("Update all dependency")
    all: Boolean = false,
)
  // format: on

object DependencyUpdateOptions {
  implicit lazy val parser: Parser[DependencyUpdateOptions] = Parser.derive
  implicit lazy val help: Help[DependencyUpdateOptions]     = Help.derive
}
