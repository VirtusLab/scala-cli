package scala.build.options

import scala.build.internal.Constants

/** Options for WebAssembly compilation and execution.
  *
  * @param enabled
  *   If true, enable WASM output (Scala.js WASM backend)
  * @param runtime
  *   The WASM runtime to use for execution (node, deno, wasmtime, wasmedge, wasmer)
  * @param denoVersion
  *   Version of Deno to download (if not found on PATH)
  */
final case class WasmOptions(
  enabled: Boolean = false,
  runtime: WasmRuntime = WasmRuntime.default,
  denoVersion: Option[String] = None
) {
  def finalDenoVersion: String =
    denoVersion.filter(_.nonEmpty).getOrElse(Constants.defaultDenoVersion)
}

object WasmOptions {
  implicit val hasHashData: HasHashData[WasmOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[WasmOptions]     = ConfigMonoid.derive
}
