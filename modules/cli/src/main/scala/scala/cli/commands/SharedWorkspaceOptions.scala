package scala.cli.commands

import caseapp._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

import scala.build.Os

// format: off
final case class SharedWorkspaceOptions(
  @Hidden
  @HelpMessage("Directory where .scala-build is written")
  @ValueDescription("path")
    workspace: Option[String] = None
) {
  // format: on

  def forcedWorkspaceOpt: Option[os.Path] =
    workspace
      .filter(_.trim.nonEmpty)
      .map(os.Path(_, Os.pwd))

}

object SharedWorkspaceOptions {
  lazy val parser: Parser[SharedWorkspaceOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[SharedWorkspaceOptions, parser.D] = parser
  implicit lazy val help: Help[SharedWorkspaceOptions]                      = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[SharedWorkspaceOptions]       = JsonCodecMaker.make
}
