package scala.cli.commands.shared

import caseapp._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

import scala.cli.commands.tags

/** Scala CLI options which aren't strictly scalac options, but directly involve the Scala compiler
  * in some way.
  */
// format: off
final case class ScalacExtraOptions(
  @Group(HelpGroup.Scala.toString)
  @HelpMessage("Show help for scalac. This is an alias for --scalac-option -help")
  @Name("helpScalac")
  @Tag(tags.inShortHelp)
    scalacHelp: Boolean = false,

  @Group(HelpGroup.Scala.toString)
  @HelpMessage("Turn verbosity on for scalac. This is an alias for --scalac-option -verbose")
  @Name("verboseScalac")
  @Tag(tags.inShortHelp)
    scalacVerbose: Boolean = false,
)
// format: on

object ScalacExtraOptions {
  implicit lazy val parser: Parser[ScalacExtraOptions]            = Parser.derive
  implicit lazy val help: Help[ScalacExtraOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[ScalacExtraOptions] = JsonCodecMaker.make
}
