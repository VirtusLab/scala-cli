package scala.cli.commands

import caseapp._
import caseapp.core.help.Help

// format: off
@HelpMessage("Generate Scaladoc documentation", "By default, Scala CLI sets common scaladoc options and this mechanism can be disabled by using `--default-scaladoc-opts:false`.")
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
  @HelpMessage("Control if scala CLI should use default options for scaladoc, true by default. Use `--default-scaladoc-opts:false` to not include default options.")
  @ExtraName("defaultScaladocOpts")
    defaultScaladocOptions: Option[Boolean] = None,
)
// format: on

object DocOptions {
  implicit lazy val parser: Parser[DocOptions] = Parser.derive
  implicit lazy val help: Help[DocOptions]     = Help.derive
}
