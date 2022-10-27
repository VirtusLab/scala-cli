package scala.cli.commands

import caseapp.core.app.Command
import caseapp.core.parser.Parser

trait RestrictableCommand[T](implicit myParser: Parser[T]) {
  self: Command[T] =>
  def isRestricted: Boolean      = false
  override def parser: Parser[T] = RestrictedCommandsParser(myParser)
}
