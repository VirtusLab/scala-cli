package scala.cli.commands

import upickle.default.{ReadWriter, macroRW}

case class BloopJson(javaOptions: List[String] = Nil)

case object BloopJson {
  implicit lazy val jsonCodec: ReadWriter[BloopJson] = macroRW
}
