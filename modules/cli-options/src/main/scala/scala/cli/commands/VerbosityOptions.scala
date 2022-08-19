package scala.cli.commands

import caseapp._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

// format: off
final case class VerbosityOptions(
  @Group("Logging")
  @HelpMessage("Increase verbosity (can be specified multiple times)")
  @Name("v")
    verbose: Int @@ Counter = Tag.of(0),
  @HelpMessage("Interactive mode")
  @Name("i")
    interactive: Option[Boolean] = None,
  @HelpMessage("Enable actionable diagnostics")
    actions: Option[Boolean] = None
) {
  // format: on

  lazy val verbosity = Tag.unwrap(verbose)

}

object VerbosityOptions {
  lazy val parser: Parser[VerbosityOptions]                           = Parser.derive
  implicit lazy val parserAux: Parser.Aux[VerbosityOptions, parser.D] = parser
  implicit lazy val help: Help[VerbosityOptions]                      = Help.derive
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
