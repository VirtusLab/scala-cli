package scala.cli.commands.shared

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import scala.cli.commands.tags

// format: off
final case class WasmOptions(
  @Group(HelpGroup.Wasm.toString)
  @Tag(tags.experimental)
  @HelpMessage("Enable WebAssembly output (Scala.js WASM backend). Uses Node.js by default. To show more options for WASM pass `--help-wasm`")
    wasm: Boolean = false,

  @Group(HelpGroup.Wasm.toString)
  @Tag(tags.experimental)
  @HelpMessage("WASM runtime to use: node (default), deno, bun")
    wasmRuntime: Option[String] = None
)
// format: on

object WasmOptions {
  implicit lazy val parser: Parser[WasmOptions]            = Parser.derive
  implicit lazy val help: Help[WasmOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[WasmOptions] = JsonCodecMaker.make
}
