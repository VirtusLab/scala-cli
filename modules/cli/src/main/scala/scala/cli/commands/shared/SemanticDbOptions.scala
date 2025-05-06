package scala.cli.commands.shared

import caseapp._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

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
  semanticDbTargetRoot: Option[String] = None,
  @Hidden
  @Tag(tags.should)
  @HelpMessage("SemanticDB source root (default to the project root directory)")
  @Name("semanticdbSourceRoot")
  @Name("semanticdbSourceroot")
  semanticDbSourceRoot: Option[String] = None
)

object SemanticDbOptions {
  implicit lazy val parser: Parser[SemanticDbOptions]            = Parser.derive
  implicit lazy val help: Help[SemanticDbOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[SemanticDbOptions] = JsonCodecMaker.make
}
