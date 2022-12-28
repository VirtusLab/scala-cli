package scala.cli.commands.version

import caseapp.*

import scala.cli.commands.shared.{HasLoggingOptions, LoggingOptions}
import scala.cli.commands.tags

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
  @HelpMessage("Show only plain version")
  @Name("cli")
    cliVersion: Boolean = false,
  @HelpMessage("Show only plain scala version")
  @Group("Version")
  @Tag(tags.implementation)
  @Name("scala")
    scalaVersion: Boolean = false
) extends HasLoggingOptions
// format: on

object VersionOptions {
  implicit lazy val parser: Parser[VersionOptions] = Parser.derive
  implicit lazy val help: Help[VersionOptions]     = Help.derive
}
