package scala.build.options

import dependency.AnyDependency

import scala.build.Positioned
import scala.build.internal.Constants

final case class ScalaOptions(
  scalaVersion: Option[MaybeScalaVersion] = None,
  scalaBinaryVersion: Option[String] = None,
  addScalaLibrary: Option[Boolean] = None,
  addScalaCompiler: Option[Boolean] = None,
  semanticDbOptions: SemanticDbOptions = SemanticDbOptions(),
  scalacOptions: ShadowingSeq[Positioned[ScalacOpt]] = ShadowingSeq.empty,
  extraScalaVersions: Set[String] = Set.empty,
  compilerPlugins: Seq[Positioned[AnyDependency]] = Nil,
  platform: Option[Positioned[Platform]] = None,
  extraPlatforms: Map[Platform, Positioned[Unit]] = Map.empty,
  defaultScalaVersion: Option[String] = None
) {
  def normalize: ScalaOptions = {
    var opt = this
    for (sv <- opt.scalaVersion.map(_.asString) if opt.extraScalaVersions.contains(sv))
      opt = opt.copy(
        extraScalaVersions = opt.extraScalaVersions - sv
      )
    for (pf <- opt.platform.map(_.value) if opt.extraPlatforms.keySet.contains(pf))
      opt = opt.copy(
        extraPlatforms = opt.extraPlatforms - pf
      )
    opt
  }

  def resolveFullScalaVersion: Option[String] =
    scalaVersion
      .flatMap(_.versionOpt) // FIXME If versionOpt is empty, the project is pure Java
      .map {
        case "3"                                     => Constants.defaultScalaVersion
        case "2.12"                                  => Constants.defaultScala212Version
        case "2.13"                                  => Constants.defaultScala213Version
        case ver @ s"$m.$mn.$p"                      => ver
        case s"3.${lts}" if lts.toLowerCase == "lts" => Constants.defaultScalaVersion
        case s"3.${any}"                             => Constants.defaultScalaVersion
        case v                                       => v
        // todo: how to get current lts and next versions
      }
}

object ScalaOptions {
  implicit val hasHashData: HasHashData[ScalaOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[ScalaOptions]     = ConfigMonoid.derive
}
