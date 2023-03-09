package scala.cli.commands.pgp

import caseapp.core.app.Command
import caseapp.core.complete.{Completer, CompletionItem}
import caseapp.core.help.{Help, HelpFormat}
import caseapp.core.parser.Parser

import scala.cli.commands.RestrictableCommand
import scala.cli.commands.util.CommandHelpers

abstract class PgpCommand[T](implicit myParser: Parser[T], help: Help[T])
    extends Command()(myParser, help)
    with CommandHelpers with RestrictableCommand[T] {

  override def scalaSpecificationLevel = SpecificationLevel.EXPERIMENTAL

  override def hidden = true
}
