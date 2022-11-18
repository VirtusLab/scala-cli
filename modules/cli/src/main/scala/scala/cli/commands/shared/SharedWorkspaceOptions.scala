package scala.cli.commands.shared

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import scala.build.Os
import scala.cli.commands.tags

// format: off
final case class SharedWorkspaceOptions(
  @Hidden
  @HelpMessage("Directory where .scala-build is written")
  @ValueDescription("path")
  @Tag(tags.implementation)
    workspace: Option[String] = None
) {
  // format: on

  def forcedWorkspaceOpt: Option[os.Path] =
    workspace
      .filter(_.trim.nonEmpty)
      .map(os.Path(_, Os.pwd))
}

object SharedWorkspaceOptions {
  implicit lazy val parser: Parser[SharedWorkspaceOptions]            = Parser.derive
  implicit lazy val help: Help[SharedWorkspaceOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[SharedWorkspaceOptions] = JsonCodecMaker.make
}
