package scala.build.options

import dependency.AnyDependency

final case class ScalaOptions(
  scalaVersion: Option[String] = None,
  scalaBinaryVersion: Option[String] = None,
  addScalaLibrary: Option[Boolean] = None,
  generateSemanticDbs: Option[Boolean] = None,
  scalacOptions: Seq[String] = Nil,
  extraScalaVersions: Set[String] = Set.empty,
  compilerPlugins: Seq[AnyDependency] = Nil,
  platform: Option[Platform] = None,
  extraPlatforms: Set[Platform] = Set.empty
) {
  def normalize: ScalaOptions = {
    var opt = this
    for (sv <- opt.scalaVersion if opt.extraScalaVersions.contains(sv))
      opt = opt.copy(
        extraScalaVersions = opt.extraScalaVersions - sv
      )
    for (pf <- opt.platform if opt.extraPlatforms.contains(pf))
      opt = opt.copy(
        extraPlatforms = opt.extraPlatforms - pf
      )
    opt
  }
}

object ScalaOptions {
  implicit val hasHashData: HasHashData[ScalaOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[ScalaOptions]     = ConfigMonoid.derive
}
