package scala.cli.commands

import caseapp._

// format: off
final case class DirectoriesOptions(
  @Recurse
    directories: SharedDirectoriesOptions = SharedDirectoriesOptions()
)
// format: on

object DirectoriesOptions {
  implicit val parser = Parser[DirectoriesOptions]
  implicit val help   = Help[DirectoriesOptions]
}
