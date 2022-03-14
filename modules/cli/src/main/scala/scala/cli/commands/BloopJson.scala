package scala.cli.commands

import upickle.default.{ReadWriter, macroRW}

final case class BloopJson(javaOptions: List[String] = Nil)

object BloopJson {
  implicit lazy val jsonCodec: ReadWriter[BloopJson] = macroRW
}
