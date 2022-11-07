package scala.cli.commands

import caseapp.*

// format: off
final case class MainClassOptions(
  @Group("Entrypoint")
  @HelpMessage("Specify which main class to run")
  @ValueDescription("main-class")
  @Name("M")
    mainClass: Option[String] = None,

  @Group("Entrypoint")
  @HelpMessage("List main classes available in the current context")
  @Name("mainClassList")
  @Name("listMainClass")
  @Name("listMainClasses")
    mainClassLs: Option[Boolean] = None
)
// format: on

object MainClassOptions {
  implicit lazy val parser: Parser[MainClassOptions] = Parser.derive
  implicit lazy val help: Help[MainClassOptions]     = Help.derive
}
