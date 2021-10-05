package scala.cli.commands

import caseapp._
import upickle.default.{ReadWriter => RW, macroRW}

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
  lazy val parser: Parser[SharedDirectoriesOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[SharedDirectoriesOptions, parser.D] = parser
  implicit lazy val help: Help[SharedDirectoriesOptions]                      = Help.derive
  implicit lazy val rw: RW[SharedCompilationServerOptions] = macroRW
}
