package scala.cli.commands

import caseapp._

// format: off
final case class SharedDependencyOptions(
  @Group("Dependency")
  @HelpMessage("Add dependencies")
  @Name("dep")
  @Name("d")
    dependency: List[String] = Nil,

  @Group("Dependency")
  @HelpMessage("Add repositories")
  @HelpMessage("")
  @Name("repo")
  @Name("r")
    repository: List[String] = Nil,
  @Group("Dependency")
  @HelpMessage("Add compiler plugins dependencies")
  compilerPlugin: List[String] = Nil
)
// format: on

object SharedDependencyOptions {
  implicit val parser = Parser[SharedDependencyOptions]
  implicit val help   = Help[SharedDependencyOptions]
}
