package scala.build.internal

import upickle.default.{ReadWriter, macroRW}

case class StableScalaVersion(scalaCliVersion: String, supportedScalaVersions: Seq[String])

case object StableScalaVersion {
  implicit lazy val jsonCodec: ReadWriter[StableScalaVersion] = macroRW
}
