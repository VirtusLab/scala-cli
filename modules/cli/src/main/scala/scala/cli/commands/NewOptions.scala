package scala.cli.commands

import caseapp._

// format: off
final case class NewOptions(
  template: Option[String] = None,
 @Name("o")
  output: Option[String] = None
) {
  // format: on
}

object NewOptions {
  implicit val parser = Parser[NewOptions]
  implicit val help   = Help[NewOptions]
}
