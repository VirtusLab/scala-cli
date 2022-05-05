package scala.build.options

final case class MaybeScalaVersion(versionOpt: Option[String] = None) {
  def asString = versionOpt.getOrElse(MaybeScalaVersion.noneStr)
}

object MaybeScalaVersion {

  private def noneStr = "none"

  def none: MaybeScalaVersion =
    MaybeScalaVersion(noneStr)

  def apply(s: String): MaybeScalaVersion =
    if (s == noneStr) MaybeScalaVersion(None)
    else MaybeScalaVersion(Some(s))

  implicit lazy val hashedType: HashedType[MaybeScalaVersion] = { v =>
    HashedType.string.hashedValue(v.versionOpt.getOrElse("no-scala-version"))
  }
  implicit lazy val hasHashData: HasHashData[MaybeScalaVersion] =
    HasHashData.derive
}
