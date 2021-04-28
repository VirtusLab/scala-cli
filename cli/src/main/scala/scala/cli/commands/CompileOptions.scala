package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

final case class CompileOptions(
  @Recurse
    shared: SharedOptions,
  @Name("p")
  @Name("classpath")
  @HelpMessage("Print resulting class path")
    classPath: Boolean = false
)

object CompileOptions {
  implicit val parser = Parser[CompileOptions]
  implicit val help = Help[CompileOptions]
}
