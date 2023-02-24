package scala.cli.commands.shared

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import scala.cli.commands.{Constants, tags}

// format: off
final case class ScalaNativeOptions(

  @Group(HelpGroup.Scala.toString)
  @HelpMessage("Enable Scala Native. To show more options for Scala Native pass `--help-native`")
  @Tag(tags.should)
    native: Boolean = false,

  @Group(HelpGroup.ScalaNative.toString)
  @Tag(tags.should)
  @HelpMessage(s"Set the Scala Native version (${Constants.scalaNativeVersion} by default).")
    nativeVersion: Option[String] = None,
  @Group(HelpGroup.ScalaNative.toString)
  @HelpMessage("Set Scala Native compilation mode")
  @Tag(tags.should)
    nativeMode: Option[String] = None,
  @Group(HelpGroup.ScalaNative.toString)
  @HelpMessage("Set the Scala Native garbage collector")
  @Tag(tags.should)
    nativeGc: Option[String] = None,

  @Group(HelpGroup.ScalaNative.toString)
  @HelpMessage("Path to the Clang command")
  @Tag(tags.implementation)
    nativeClang: Option[String] = None,
  @Group(HelpGroup.ScalaNative.toString)
  @HelpMessage("Path to the Clang++ command")
   @Tag(tags.implementation)
    nativeClangpp: Option[String] = None,

  @Group(HelpGroup.ScalaNative.toString)
  @HelpMessage("Extra options passed to `clang` verbatim during linking")
   @Tag(tags.should)
    nativeLinking: List[String] = Nil,
  @Group(HelpGroup.ScalaNative.toString)
  @HelpMessage("Use default linking settings")
  @Hidden
  @Tag(tags.implementation)
    nativeLinkingDefaults: Option[Boolean] = None, //TODO does it even work when we default it to true while handling?

  @Group(HelpGroup.ScalaNative.toString)
  @HelpMessage("List of compile options")
   @Tag(tags.should)
    nativeCompile: List[String] = Nil,

  @Group(HelpGroup.ScalaNative.toString)
  @Hidden
  @HelpMessage("Use default compile options")
   @Tag(tags.implementation)
    nativeCompileDefaults: Option[Boolean] = None, //TODO does it even work when we default it to true while handling?

  @Group(HelpGroup.ScalaNative.toString)
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
