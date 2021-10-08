package scala.cli.commands

import caseapp._

final case class CrossOptions(
  cross: Option[Boolean] = None
)

object CrossOptions {
  implicit lazy val parser                   = Parser[CrossOptions]
  implicit lazy val help: Help[CrossOptions] = Help.derive
}
