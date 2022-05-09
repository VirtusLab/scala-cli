package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

// format: off
@HelpMessage("Generate Scaladoc documentation")
final case class DocOptions(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  @Group("Doc")
  @HelpMessage("Set the destination path")
  @Name("o")
    output: Option[String] = None,
  @Group("Doc")
  @HelpMessage("Overwrite the destination directory, if it exists")
  @Name("f")
    force: Boolean = false,
  @Group("Doc")
  @HelpMessage("Use default scaladoc options")
  @ExtraName("defaultScaladocOpts")
    defaultScaladocOptions: Option[Boolean] = None,
)
// format: on

object DocOptions {
  implicit lazy val parser: Parser[DocOptions] = Parser.derive
  implicit lazy val help: Help[DocOptions]     = Help.derive
}
