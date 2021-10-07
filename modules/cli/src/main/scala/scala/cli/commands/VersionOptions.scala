package scala.cli.commands

import caseapp._

@HelpMessage("Print scala-cli version")
final case class VersionOptions()

object VersionOptions {
  implicit val parser = Parser[VersionOptions]
  implicit val help   = Help[VersionOptions]
}
