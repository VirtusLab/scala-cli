package scala.cli.commands.fix

import caseapp.*
import caseapp.core.help.Help

import scala.build.internal.Constants
import scala.cli.commands.shared.{HasSharedOptions, SharedOptions}

// format: off
@HelpMessage("Run fixes for a Scala CLI project")
final case class FixOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @HelpMessage(s"Move all using directives to the ${Constants.projectFileName} file")
    migrateDirectives: Option[Boolean] = None
) extends HasSharedOptions
// format: on

object FixOptions {
  implicit lazy val parser: Parser[FixOptions] = Parser.derive
  implicit lazy val help: Help[FixOptions]     = Help.derive
}
