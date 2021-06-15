package scala.cli.commands

import caseapp._
import caseapp.core.help.Help
import scala.build.Build

@HelpMessage("Compile Scala code")
final case class CompileOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),

  @Name("p")
  @Name("classpath")
  @HelpMessage("Print resulting class path")
    classPath: Boolean = false
) {

  def buildOptions: Build.Options =
    shared.buildOptions(jmhOptions = None, jmhVersion = None)
}

object CompileOptions {
  implicit val parser = Parser[CompileOptions]
  implicit val help = Help[CompileOptions]
}
