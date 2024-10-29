package scala.build.internal

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import coursier.core.Version

final case class StableScalaVersion(
  scalaCliVersion: String,
  supportedScalaVersions: Seq[String]
) {
  lazy val scalaCliVersion0 = Version(scalaCliVersion)
}

object StableScalaVersion {
  val seqCodec: JsonValueCodec[Seq[StableScalaVersion]] = JsonCodecMaker.make
}
