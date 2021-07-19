package scala.cli.commands

import caseapp._

final case class SharedDirectoriesOptions() {
  lazy val directories = scala.build.Directories.default()
}
