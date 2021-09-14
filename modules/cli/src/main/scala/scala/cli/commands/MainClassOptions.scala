package scala.cli.commands

import caseapp._

// format: off
final case class MainClassOptions(
  @Group("Entrypoint")
  @HelpMessage("Specify which main class to run")
  @ValueDescription("main-class")
  @Name("M")
    mainClass: Option[String] = None
)
// format: on

object MainClassOptions {
  implicit val parser = Parser[MainClassOptions]
  implicit val help   = Help[MainClassOptions]
}
