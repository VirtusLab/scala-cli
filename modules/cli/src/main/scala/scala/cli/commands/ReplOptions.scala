package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

final case class ReplOptions(
  @Recurse
    shared: SharedOptions,
  ammonite: Option[String] = None
) {
  def ammoniteVersion: String =
    ammonite.getOrElse {
      "2.3.8-58-aa8b2ab1" // TODO Get via scala.cli.internal.Constants
    }
}

object ReplOptions {
  implicit val parser = Parser[ReplOptions]
  implicit val help = Help[ReplOptions]
}
