package scala.cli.commands.shared

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import scala.build.Os
import scala.cli.commands.tags

// format: off
final case class SharedDirectoriesOptions(
  @Name("home")
  @HelpMessage("Override the path to user's home directory")
  @Tag(tags.implementation)
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
  implicit lazy val parser: Parser[SharedDirectoriesOptions]            = Parser.derive
  implicit lazy val help: Help[SharedDirectoriesOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[SharedDirectoriesOptions] = JsonCodecMaker.make
}
