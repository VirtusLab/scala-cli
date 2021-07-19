package scala.cli.commands

import caseapp._

final case class DirectoriesOptions(
  @Recurse
    directories: SharedDirectoriesOptions = SharedDirectoriesOptions()
)

object DirectoriesOptions {
  implicit val parser = Parser[DirectoriesOptions]
  implicit val help = Help[DirectoriesOptions]
}
