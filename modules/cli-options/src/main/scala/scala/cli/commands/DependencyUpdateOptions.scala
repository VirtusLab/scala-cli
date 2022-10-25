package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

import scala.cli.commands.common.HasSharedOptions

// format: off
@HelpMessage("Update dependencies in project")
final case class DependencyUpdateOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Group("DependencyUpdate")
  @HelpMessage("Update all dependencies if newer version was released")
    all: Boolean = false,
) extends HasSharedOptions
  // format: on

object DependencyUpdateOptions {
  implicit lazy val parser: Parser[DependencyUpdateOptions] = Parser.derive
  implicit lazy val help: Help[DependencyUpdateOptions]     = Help.derive
}
