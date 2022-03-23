package scala.build.bsp

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

case class IdeInputs(mainScopeSources: List[String], testScopeSources: List[String])
object IdeInputs {
  implicit lazy val codec: JsonValueCodec[IdeInputs] = JsonCodecMaker.make[IdeInputs]
}
