package scala.build.internal

import upickle.default.{ReadWriter, macroRW}

final case class StableScalaVersion(scalaCliVersion: String, supportedScalaVersions: Seq[String])

object StableScalaVersion {
  implicit lazy val jsonCodec: ReadWriter[StableScalaVersion] = macroRW
}
