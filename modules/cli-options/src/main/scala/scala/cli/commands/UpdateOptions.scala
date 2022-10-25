package scala.cli.commands

import caseapp._

import scala.cli.commands.common.HasLoggingOptions
import scala.cli.signing.shared.PasswordOption
import scala.cli.signing.util.ArgParsers._

// format: off
@HelpMessage("Update scala-cli - only works when installed by the installation script")
final case class UpdateOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
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
  @Hidden
  @HelpMessage(HelpMessages.passwordOption)
    ghToken: Option[PasswordOption] = None
) extends HasLoggingOptions
// format: on

object UpdateOptions {
  implicit lazy val parser: Parser[UpdateOptions] = Parser.derive
  implicit lazy val help: Help[UpdateOptions]     = Help.derive
}
