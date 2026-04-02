package scala.build.options

/** Options for WebAssembly compilation and execution.
  *
  * @param enabled
  *   If true, enable WASM output (Scala.js WASM backend)
  * @param runtime
  *   The WASM runtime to use for execution (node, deno)
  */
final case class WasmOptions(
  enabled: Boolean = false,
  runtime: WasmRuntime = WasmRuntime.default
)

object WasmOptions {
  implicit val hasHashData: HasHashData[WasmOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[WasmOptions]     = ConfigMonoid.derive
}
