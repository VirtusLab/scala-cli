package scala.cli.commands

import caseapp._

@HelpMessage("Print details about this application")
final case class AboutOptions()

object AboutOptions {
  implicit val parser = Parser[AboutOptions]
  implicit val help = Help[AboutOptions]
}
