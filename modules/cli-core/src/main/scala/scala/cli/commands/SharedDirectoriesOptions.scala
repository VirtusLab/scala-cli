package scala.cli.commands

import caseapp._
import scala.build.Directories

final case class SharedDirectoriesOptions() {
  lazy val directories = Directories.default()
}
