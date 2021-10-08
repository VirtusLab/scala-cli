package scala.cli.commands

import caseapp._

// format: off
final case class DirectoriesOptions(
  @Recurse
    directories: SharedDirectoriesOptions = SharedDirectoriesOptions()
)
// format: on

object DirectoriesOptions {
  implicit lazy val parser                         = Parser[DirectoriesOptions]
  implicit lazy val help: Help[DirectoriesOptions] = Help.derive
}
