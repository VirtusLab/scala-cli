package scala.cli.commands.dependencyupdate

import caseapp.*
import caseapp.core.help.Help

import scala.cli.commands.shared.{HasSharedOptions, HelpGroup, SharedOptions}
import scala.cli.commands.tags

// format: off
@HelpMessage("Update dependency directives in the project")
final case class DependencyUpdateOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Group(HelpGroup.Dependency.toString)
  @HelpMessage("Update all dependencies if a newer version was released")
  @Tag(tags.restricted)
  @Tag(tags.inShortHelp)
    all: Boolean = false,
) extends HasSharedOptions
  // format: on

object DependencyUpdateOptions {
  implicit lazy val parser: Parser[DependencyUpdateOptions] = Parser.derive
  implicit lazy val help: Help[DependencyUpdateOptions]     = Help.derive
}
