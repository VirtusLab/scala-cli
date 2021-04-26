package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

final case class RunOptions(
  @Recurse
    shared: SharedOptions,
  @Name("M")
    mainClass: Option[String] = None
)

object RunOptions {
  implicit val parser = Parser[RunOptions]
  implicit val help = Help[RunOptions]
}
