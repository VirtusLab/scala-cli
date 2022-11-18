package scala.cli.commands

import caseapp.*

import scala.cli.commands.common.HasLoggingOptions
import scala.cli.signing.shared.PasswordOption
import scala.cli.signing.util.ArgParsers.*

// format: off
@HelpMessage("Update scala-cli - only works when installed by the installation script")
final case class UpdateOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Hidden
  @Group("Update")
  @HelpMessage("Binary name")
  @Tag(tags.implementation)
    binaryName: String = "scala-cli",
  @Hidden
  @Group("Update")
  @HelpMessage("Binary directory")
  @Tag(tags.implementation)
    binDir: Option[String] = None,
  @Name("f")
  @HelpMessage("Force update scala-cli if is outdated")
  @Tag(tags.implementation)
    force: Boolean = false,
  @Hidden
  @Tag(tags.implementation)
    isInternalRun: Boolean = false,
  @Hidden
  @HelpMessage(HelpMessages.passwordOption)
  @Tag(tags.implementation)
    ghToken: Option[PasswordOption] = None
) extends HasLoggingOptions
// format: on

object UpdateOptions {
  implicit lazy val parser: Parser[UpdateOptions] = Parser.derive
  implicit lazy val help: Help[UpdateOptions]     = Help.derive
}
