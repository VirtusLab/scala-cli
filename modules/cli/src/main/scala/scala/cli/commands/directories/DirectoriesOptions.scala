package scala.cli.commands.directories

import caseapp.*

import scala.cli.ScalaCli.fullRunnerName
import scala.cli.commands.shared.{GlobalOptions, HasGlobalOptions}

// format: off
@HelpMessage(s"Prints directories used by $fullRunnerName.")
final case class DirectoriesOptions(
@Recurse
  global: GlobalOptions = GlobalOptions(),
) extends HasGlobalOptions
// format: on

object DirectoriesOptions {
  implicit lazy val parser: Parser[DirectoriesOptions] = Parser.derive
  implicit lazy val help: Help[DirectoriesOptions]     = Help.derive
}
