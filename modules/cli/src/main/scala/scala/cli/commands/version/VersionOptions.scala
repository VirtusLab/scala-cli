package scala.cli.commands.version

import caseapp.*

import scala.cli.ScalaCli.fullRunnerName
import scala.cli.commands.shared.{HasLoggingOptions, HelpMessages, LoggingOptions}
import scala.cli.commands.tags
import scala.cli.signing.shared.PasswordOption
import scala.cli.signing.util.ArgParsers.*

// format: off
@HelpMessage(
  """|Print the version of the scala runner and the default version of Scala (unless specified in the project).
     |
     |The version of the scala runner is the version of the command-line tool that runs Scala programs, which
     |is distinct from the Scala version of a program. We recommend you specify the version of Scala of a
     |program in the program itself (via a configuration directive). Otherwise, the runner falls back to the default
     |Scala version defined by the runner.
     |""".stripMargin
)
final case class VersionOptions(
  @Recurse
    logging: LoggingOptions = LoggingOptions(),
  @Tag(tags.implementation)
  @Group("Version")
  @HelpMessage(s"Show plain $fullRunnerName version only")
  @Name("cli")
    cliVersion: Boolean = false,
  @Group("Version")
  @HelpMessage("Show plain Scala version only")
  @Tag(tags.implementation)
  @Name("scala")
    scalaVersion: Boolean = false,
  @Hidden
  @HelpMessage(HelpMessages.passwordOption)
  @Tag(tags.implementation)
    ghToken: Option[PasswordOption] = None,
  @Group("Version")
  @HelpMessage(s"Don't check for the newest available $fullRunnerName version upstream")
    offline: Boolean = false
) extends HasLoggingOptions
// format: on

object VersionOptions {
  implicit lazy val parser: Parser[VersionOptions] = Parser.derive
  implicit lazy val help: Help[VersionOptions]     = Help.derive
}
