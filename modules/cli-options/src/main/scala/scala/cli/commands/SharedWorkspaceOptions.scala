package scala.cli.commands

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

// format: off
final case class SharedWorkspaceOptions(
  @Hidden
  @HelpMessage("Directory where .scala-build is written")
  @ValueDescription("path")
    workspace: Option[String] = None
)
// format: on

object SharedWorkspaceOptions {
  lazy val parser: Parser[SharedWorkspaceOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[SharedWorkspaceOptions, parser.D] = parser
  implicit lazy val help: Help[SharedWorkspaceOptions]                      = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[SharedWorkspaceOptions]       = JsonCodecMaker.make
}
