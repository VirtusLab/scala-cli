package scala.cli.commands.shared

import caseapp.*
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

import scala.cli.commands.tags

// format: off
final case class WasmOptions(
  @Group(HelpGroup.Scala.toString)
  @Tag(tags.experimental)
  @HelpMessage("Enable WebAssembly output (Scala.js WASM backend). Uses Node.js by default. To show more options for WASM pass `--help-wasm`")
    wasm: Boolean = false,

  @Group(HelpGroup.Wasm.toString)
  @Tag(tags.experimental)
  @HelpMessage("WASM runtime to use: node (default), deno. Standalone runtimes (wasmtime, wasmedge) planned for future releases.")
    wasmRuntime: Option[String] = None,

  @Group(HelpGroup.Wasm.toString)
  @Tag(tags.experimental)
  @HelpMessage("Version of Deno to use. If Deno is not found on PATH, it will be downloaded automatically.")
    denoVersion: Option[String] = None
)
// format: on

object WasmOptions {
  implicit lazy val parser: Parser[WasmOptions]            = Parser.derive
  implicit lazy val help: Help[WasmOptions]                = Help.derive
  implicit lazy val jsonCodec: JsonValueCodec[WasmOptions] = JsonCodecMaker.make
}
