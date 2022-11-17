package scala.cli.commands

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

// format: off
final case class SharedDirectoriesOptions(
  @Name("home")
    homeDirectory: Option[String] = None
)
// format: on

object SharedDirectoriesOptions {
  implicit lazy val parser: Parser[SharedDirectoriesOptions]            = Parser.derive
  implicit lazy val help: Help[SharedDirectoriesOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[SharedDirectoriesOptions] = JsonCodecMaker.make
}
