package scala.cli.commands.shared

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

/** Scala CLI options which aren't strictly scalac options, but directly involve the Scala compiler
  * in some way.
  */
// format: off
final case class ScalacExtraOptions(
  @Group(HelpGroup.Scala.toString)
  @HelpMessage("Show help for scalac. This is an alias for --scalac-option -help")
  @Name("helpScalac")
    scalacHelp: Boolean = false,

  @Group(HelpGroup.Scala.toString)
  @HelpMessage("Turn verbosity on for scalac. This is an alias for --scalac-option -verbose")
  @Name("verboseScalac")
    scalacVerbose: Boolean = false,
)
// format: on

object ScalacExtraOptions {
  implicit lazy val parser: Parser[ScalacExtraOptions]            = Parser.derive
  implicit lazy val help: Help[ScalacExtraOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[ScalacExtraOptions] = JsonCodecMaker.make
}
