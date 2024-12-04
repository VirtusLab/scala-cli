package scala.cli.commands.bloop

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

final case class BloopJson(javaOptions: List[String] = Nil)

object BloopJson {
  val codec: JsonValueCodec[BloopJson] = JsonCodecMaker.make
}
