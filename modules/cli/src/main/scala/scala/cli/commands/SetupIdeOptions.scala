package scala.cli.commands

import caseapp._

// format: off
final case class SetupIdeOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Name("bspDir")
    bspDirectory: Option[String] = None,
  @Name("name")
    bspName: Option[String] = None,
  charset: Option[String] = None
)
// format: on

object SetupIdeOptions {
  implicit val parser = Parser[SetupIdeOptions]
  implicit val help   = Help[SetupIdeOptions]
}
