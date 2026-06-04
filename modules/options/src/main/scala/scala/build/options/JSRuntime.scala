package scala.build.options

import java.util.Locale

/** JS runtime used to run the linked output. Currently consulted only for Wasm execution; each
  * embeds a Wasm engine (V8 for Node.js/Deno, JavaScriptCore for Bun).
  */
sealed abstract class JSRuntime(val name: String)

object JSRuntime {
  case object Node extends JSRuntime("node")
  case object Deno extends JSRuntime("deno")
  case object Bun  extends JSRuntime("bun")

  val all: Seq[JSRuntime] = Seq(Node, Deno, Bun)

  def default: JSRuntime = Node

  def parse(s: String): Option[JSRuntime] =
    s.trim.toLowerCase(Locale.ROOT) match {
      case "node" | "nodejs" => Some(Node)
      case "deno"            => Some(Deno)
      case "bun"             => Some(Bun)
      case _                 => None
    }

  implicit val hashedType: HashedType[JSRuntime] = runtime => runtime.name

  implicit val hasHashData: HasHashData[JSRuntime] = HasHashData.asIs

  implicit val monoid: ConfigMonoid[JSRuntime] = ConfigMonoid.instance[JSRuntime](default) {
    (a, b) => if (b == default) a else b
  }
}
