package scala.cli.commands.pgp

import caseapp.core.app.Command
import caseapp.core.complete.{Completer, CompletionItem}
import caseapp.core.help.{Help, HelpFormat}
import caseapp.core.parser.Parser

import scala.build.Logger
import scala.cli.commands.RestrictableCommand
import scala.cli.commands.util.CommandHelpers
import scala.cli.internal.CliLogger

abstract class PgpCommand[T](implicit myParser: Parser[T], help: Help[T])
    extends Command()(myParser, help)
    with CommandHelpers with RestrictableCommand[T] {

  override def scalaSpecificationLevel = SpecificationLevel.EXPERIMENTAL

  override def shouldSuppressExperimentalFeatureWarnings: Boolean =
    false // TODO add handling for scala-cli-signing

  override def logger: Logger = CliLogger.default // TODO add handling for scala-cli-signing

  override def hidden = true
}
