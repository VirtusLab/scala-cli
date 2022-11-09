package scala.cli.commands.shared

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import scala.cli.commands.tags

// format: off
final case class SharedDirectoriesOptions(
  @Name("home")
  @HelpMessage("Override the path to user's home directory")
  @Tag(tags.implementation)
    homeDirectory: Option[String] = None
)
// format: on

object SharedDirectoriesOptions {
  implicit lazy val parser: Parser[SharedDirectoriesOptions]            = Parser.derive
  implicit lazy val help: Help[SharedDirectoriesOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[SharedDirectoriesOptions] = JsonCodecMaker.make
}
