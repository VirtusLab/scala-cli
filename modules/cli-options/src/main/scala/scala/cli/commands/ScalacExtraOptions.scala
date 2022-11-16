package scala.cli.commands

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

/** Scala CLI options which aren't strictly scalac options, but directly involve the Scala compiler
  * in some way.
  */
// format: off
final case class ScalacExtraOptions(
  @Group("Scala")
  @HelpMessage("Show help for scalac. This is an alias for --scalac-option -help")
  @Tag(tags.internal)
  @Name("helpScalac")
    scalacHelp: Boolean = false,

  @Group("Scala")
  @HelpMessage("Turn verbosity on for scalac. This is an alias for --scalac-option -verbose")
  @Tag(tags.internal)
  @Name("verboseScalac")
    scalacVerbose: Boolean = false,
)
// format: on

object ScalacExtraOptions {
  lazy val parser: Parser[ScalacExtraOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[ScalacExtraOptions, parser.D] = parser
  implicit lazy val help: Help[ScalacExtraOptions]                      = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[ScalacExtraOptions]       = JsonCodecMaker.make
}
