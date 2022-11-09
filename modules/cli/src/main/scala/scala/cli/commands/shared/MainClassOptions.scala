package scala.cli.commands.shared

import caseapp.*

import scala.cli.commands.tags

// format: off
final case class MainClassOptions(
  @Group("Entrypoint")
  @HelpMessage("Specify which main class to run")
  @ValueDescription("main-class")
  @Tag(tags.must)
  @Name("M")
    mainClass: Option[String] = None,

  @Group("Entrypoint")
  @HelpMessage("List main classes available in the current context")
  @Name("mainClassList")
  @Name("listMainClass")
  @Name("listMainClasses")
  @Tag(tags.should)
    mainClassLs: Option[Boolean] = None
)
// format: on

object MainClassOptions {
  implicit lazy val parser: Parser[MainClassOptions] = Parser.derive
  implicit lazy val help: Help[MainClassOptions]     = Help.derive
}
