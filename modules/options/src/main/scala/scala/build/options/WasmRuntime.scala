package scala.build.options

import java.util.Locale

/** Represents available WebAssembly runtimes for execution.
  *
  * JS-based runtimes (work now with Scala.js WASM backend):
  *   - Node: Uses Node.js (V8 engine) with JavaScript loader
  *   - Deno: Uses Deno (V8 engine) with ES module support
  *
  * Standalone runtimes (future, requires upstream Scala.js standalone WASM support):
  *   - Wasmtime: Primary standalone target, full WasmGC + Component Model
  *   - WasmEdge: Secondary standalone target, CNCF cloud-native runtime
  *   - Wasmer: Placeholder, no WasmGC support yet
  */
sealed abstract class WasmRuntime(val name: String) {
  def isJsBased: Boolean = this match {
    case WasmRuntime.Node | WasmRuntime.Deno => true
    case _                                   => false
  }
  def isStandalone: Boolean = !isJsBased
}

object WasmRuntime {
  // JS-based runtimes (work now)
  case object Node extends WasmRuntime("node")
  case object Deno extends WasmRuntime("deno")
  // Standalone runtimes (future - requires upstream Scala.js standalone WASM support)
  case object Wasmtime extends WasmRuntime("wasmtime")
  case object WasmEdge extends WasmRuntime("wasmedge")
  case object Wasmer   extends WasmRuntime("wasmer")

  val all: Seq[WasmRuntime] = Seq(Node, Deno, Wasmtime, WasmEdge, Wasmer)

  def default: WasmRuntime = Node

  def parse(s: String): Option[WasmRuntime] =
    s.trim.toLowerCase(Locale.ROOT) match {
      case "node" | "nodejs" => Some(Node)
      case "deno"            => Some(Deno)
      case "wasmtime"        => Some(Wasmtime)
      case "wasmedge"        => Some(WasmEdge)
      case "wasmer"          => Some(Wasmer)
      case _                 => None
    }

  implicit val hashedType: HashedType[WasmRuntime] = runtime => runtime.name

  implicit val hasHashData: HasHashData[WasmRuntime] = HasHashData.asIs

  implicit val monoid: ConfigMonoid[WasmRuntime] = ConfigMonoid.instance[WasmRuntime](default) {
    (a, b) => if (b == default) a else b
  }
}
