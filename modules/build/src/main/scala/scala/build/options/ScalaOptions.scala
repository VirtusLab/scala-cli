package scala.build.options

import dependency.AnyDependency

import scala.build.Positioned

final case class ScalaOptions(
  scalaVersion: Option[String] = None,
  scalaBinaryVersion: Option[String] = None,
  addScalaLibrary: Option[Boolean] = None,
  generateSemanticDbs: Option[Boolean] = None,
  scalacOptions: ShadowingSeq[ScalacOpt] = ShadowingSeq.empty,
  extraScalaVersions: Set[String] = Set.empty,
  compilerPlugins: Seq[Positioned[AnyDependency]] = Nil,
  platform: Option[Positioned[Platform]] = None,
  extraPlatforms: Map[Platform, Positioned[Unit]] = Map.empty,
  supportedScalaVersionsUrl: Option[String] = None
) {

  lazy val scalaVersionsUrl = supportedScalaVersionsUrl.getOrElse(
    "https://github.com/VirtuslabRnD/scala-cli-scala-versions/raw/master/scala-versions-v1.json"
  )

  def normalize: ScalaOptions = {
    var opt = this
    for (sv <- opt.scalaVersion if opt.extraScalaVersions.contains(sv))
      opt = opt.copy(
        extraScalaVersions = opt.extraScalaVersions - sv
      )
    for (pf <- opt.platform.map(_.value) if opt.extraPlatforms.keySet.contains(pf))
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
