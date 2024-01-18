package scala.cli.commands.shared

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import scala.cli.commands.tags

case class SemanticDbOptions(
  @Hidden
  @Tag(tags.should)
  @HelpMessage("Generate SemanticDBs")
  @Name("semanticdb")
  semanticDb: Option[Boolean] = None,
  @Hidden
  @Tag(tags.should)
  @HelpMessage("SemanticDB target root (default to the compiled classes destination directory)")
  @Name("semanticdbTargetRoot")
  @Name("semanticdbTargetroot")
  semanticDbTargetRoot: Option[String] = None
)

object SemanticDbOptions {
  implicit lazy val parser: Parser[SemanticDbOptions]            = Parser.derive
  implicit lazy val help: Help[SemanticDbOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[SemanticDbOptions] = JsonCodecMaker.make
}
