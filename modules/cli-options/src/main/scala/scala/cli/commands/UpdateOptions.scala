package scala.cli.commands

import caseapp._

// format: off
@HelpMessage("Update scala-cli - it works only for installation script")
final case class UpdateOptions(
  @Recurse
    verbosity: VerbosityOptions = VerbosityOptions(),
  @Hidden
  @Group("Update")
  @HelpMessage("Binary name")
    binaryName: String = "scala-cli",
  @Hidden
  @Group("Update")
  @HelpMessage("Binary directory")
    binDir: Option[String] = None,
  @Name("f")
  @HelpMessage("Force update scala-cli if is outdated")
    force: Boolean = false,
  @Hidden
    isInternalRun: Boolean = false,
)
// format: on

object UpdateOptions {
  implicit lazy val parser: Parser[UpdateOptions] = Parser.derive
  implicit lazy val help: Help[UpdateOptions]     = Help.derive
}
