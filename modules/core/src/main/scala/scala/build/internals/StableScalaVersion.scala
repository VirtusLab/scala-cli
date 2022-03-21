package scala.build.internal

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

final case class StableScalaVersion(scalaCliVersion: String, supportedScalaVersions: Seq[String])

object StableScalaVersion {
  val seqCodec: JsonValueCodec[Seq[StableScalaVersion]] = JsonCodecMaker.make
}
