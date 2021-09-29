package scala.cli.commands

import caseapp._

import scala.build.Os

// format: off
final case class SharedDirectoriesOptions(
  @Name("home")
    homeDirectory: Option[String] = None
) {
  // format: on

  lazy val directories: scala.build.Directories =
    homeDirectory.filter(_.trim.nonEmpty) match {
      case None =>
        scala.build.Directories.default()
      case Some(homeDir) =>
        val homeDir0 = os.Path(homeDir, Os.pwd)
        scala.build.Directories.under(homeDir0)
    }
}

object SharedDirectoriesOptions {
  implicit val parser = Parser[SharedDirectoriesOptions]
  implicit val help   = Help[SharedDirectoriesOptions]
}
