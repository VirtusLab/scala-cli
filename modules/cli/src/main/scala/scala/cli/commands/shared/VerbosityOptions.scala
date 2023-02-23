package scala.cli.commands.shared

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import scala.build.interactive.Interactive.*
import scala.cli.commands.tags

// format: off
final case class VerbosityOptions(
  @Group(HelpGroup.Logging.toString)
  @HelpMessage("Increase verbosity (can be specified multiple times)")
  @Tag(tags.implementation)
  @Name("v")
  @Name("-verbose")
    verbose: Int @@ Counter = Tag.of(0),
  @Group(HelpGroup.Logging.toString)
  @HelpMessage("Interactive mode")
  @Name("i")
  @Tag(tags.implementation)
    interactive: Option[Boolean] = None,
  @Group(HelpGroup.Logging.toString)
  @HelpMessage("Enable actionable diagnostics")
  @Tag(tags.implementation)
    actions: Option[Boolean] = None
) {
  // format: on

  lazy val verbosity = Tag.unwrap(verbose)

  def interactiveInstance(forceEnable: Boolean = false) =
    if (interactive.getOrElse(forceEnable)) InteractiveAsk else InteractiveNop
}

object VerbosityOptions {
  implicit lazy val parser: Parser[VerbosityOptions] = Parser.derive
  implicit lazy val help: Help[VerbosityOptions]     = Help.derive
  implicit val rwCounter: JsonValueCodec[Int @@ Counter] =
    new JsonValueCodec[Int @@ Counter] {
      private val intCodec: JsonValueCodec[Int] = JsonCodecMaker.make
      def decodeValue(in: JsonReader, default: Int @@ Counter) =
        Tag.of(intCodec.decodeValue(in, Tag.unwrap(default)))
      def encodeValue(x: Int @@ Counter, out: JsonWriter): Unit =
        intCodec.encodeValue(Tag.unwrap(x), out)
      def nullValue: Int @@ Counter =
        Tag.of(0)
    }
  implicit lazy val jsonCodec: JsonValueCodec[VerbosityOptions] = JsonCodecMaker.make
}
