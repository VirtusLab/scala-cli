package scala.cli.commands.pgp

import caseapp.*

final case class DummyOptions()

object DummyOptions {
  implicit lazy val parser: Parser[DummyOptions] = Parser.derive
  implicit lazy val help: Help[DummyOptions]     = Help.derive
}
