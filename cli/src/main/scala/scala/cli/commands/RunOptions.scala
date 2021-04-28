package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

final case class RunOptions(
  @Recurse
    shared: SharedOptions,
  @Name("M")
    mainClass: Option[String] = None
) {
  def retainedMainClass: Option[String] =
    // TODO Warn if users passed a main class along with --jmh
    if (shared.enableJmh) Some("org.openjdk.jmh.Main")
    else mainClass
}

object RunOptions {
  implicit val parser = Parser[RunOptions]
  implicit val help = Help[RunOptions]
}
