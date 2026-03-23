package scala.build.options

import java.util.Locale

/** Represents available WebAssembly runtimes for execution.
  *
  * JS-based runtimes (work now with Scala.js WASM backend):
  *   - Node: Uses Node.js (V8 engine) with JavaScript loader
  *   - Deno: Uses Deno (V8 engine) with ES module support
  */
sealed abstract class WasmRuntime(val name: String)

object WasmRuntime {
  case object Node extends WasmRuntime("node")
  case object Deno extends WasmRuntime("deno")

  val all: Seq[WasmRuntime] = Seq(Node, Deno)

  def default: WasmRuntime = Node

  def parse(s: String): Option[WasmRuntime] =
    s.trim.toLowerCase(Locale.ROOT) match {
      case "node" | "nodejs" => Some(Node)
      case "deno"            => Some(Deno)
      case _                 => None
    }

  implicit val hashedType: HashedType[WasmRuntime] = runtime => runtime.name

  implicit val hasHashData: HasHashData[WasmRuntime] = HasHashData.asIs

  implicit val monoid: ConfigMonoid[WasmRuntime] = ConfigMonoid.instance[WasmRuntime](default) {
    (a, b) => if (b == default) a else b
  }
}
