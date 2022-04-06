package scala.build.bsp

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker

/** Model for the `.scala-build/ide-inputs.json` file.
  * @param args
  *   validated input (sources) arguments passed to the `setup-ide` command.
  */
case class IdeInputs(args: Seq[String])
object IdeInputs {
  implicit lazy val codec: JsonValueCodec[IdeInputs] = JsonCodecMaker.make[IdeInputs]
}
