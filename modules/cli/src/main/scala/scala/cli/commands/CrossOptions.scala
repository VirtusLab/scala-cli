package scala.cli.commands

import caseapp._

final case class CrossOptions(
  cross: Option[Boolean] = None
)

object CrossOptions {
  implicit val parser = Parser[CrossOptions]
  implicit val help   = Help[CrossOptions]
}
