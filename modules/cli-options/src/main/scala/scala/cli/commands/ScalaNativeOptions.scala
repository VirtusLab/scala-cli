package scala.cli.commands

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import scala.cli.commands.Constants

// format: off
final case class ScalaNativeOptions(

  @Group("Scala")
  @HelpMessage("Enable Scala Native. To show more options for Scala Native pass `--help-native`")
  @Tag(tags.should)
    native: Boolean = false,

  @Group("Scala Native")
  @Tag(tags.should)
  @HelpMessage(s"Set the Scala Native version (${Constants.scalaNativeVersion} by default).")
    nativeVersion: Option[String] = None,
  @Group("Scala Native")
  @HelpMessage("Set Scala Native compilation mode")
  @Tag(tags.should)
    nativeMode: Option[String] = None,
  @Group("Scala Native")
  @HelpMessage("Set the Scala Native garbage collector")
  @Tag(tags.should)
    nativeGc: Option[String] = None,

  @Group("Scala Native")
  @HelpMessage("Path to the Clang command")
  @Tag(tags.implementation)
    nativeClang: Option[String] = None,
  @Group("Scala Native")
  @HelpMessage("Path to the Clang++ command")
   @Tag(tags.implementation)
    nativeClangpp: Option[String] = None,

  @Group("Scala Native")
  @HelpMessage("Extra options passed to `clang` verbatim during linking")
   @Tag(tags.should)
    nativeLinking: List[String] = Nil,
  @Group("Scala Native")
  @HelpMessage("Use default linking settings")
  @Hidden
  @Tag(tags.implementation)
    nativeLinkingDefaults: Option[Boolean] = None, //TODO does it even work when we default it to true while handling?

  @Group("Scala Native")
  @HelpMessage("List of compile options")
   @Tag(tags.should)
    nativeCompile: List[String] = Nil,

  @Group("Scala Native")
  @Hidden
  @HelpMessage("Use default compile options")
   @Tag(tags.implementation)
    nativeCompileDefaults: Option[Boolean] = None, //TODO does it even work when we default it to true while handling?

  @Group("Scala Native")
  @HelpMessage("Embed resources into the Scala Native binary (can be read with the Java resources API)")
   @Tag(tags.should)
    embedResources: Option[Boolean] = None

)
// format: on

object ScalaNativeOptions {
  implicit lazy val parser: Parser[ScalaNativeOptions]            = Parser.derive
  implicit lazy val help: Help[ScalaNativeOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[ScalaNativeOptions] = JsonCodecMaker.make
}
