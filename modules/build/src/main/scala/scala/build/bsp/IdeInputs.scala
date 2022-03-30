package scala.build.bsp

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

case class IdeInputs(args: Seq[String])
object IdeInputs {
  implicit lazy val codec: JsonValueCodec[IdeInputs] = JsonCodecMaker.make[IdeInputs]
}
