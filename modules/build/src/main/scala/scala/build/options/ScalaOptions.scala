package scala.build.options

final case class ScalaOptions(
  scalaVersion: Option[String] = None,
  scalaBinaryVersion: Option[String] = None,
  addScalaLibrary: Option[Boolean] = None,
  generateSemanticDbs: Option[Boolean] = None,
  scalacOptions: Seq[String] = Nil,
  extraScalaVersions: Set[String] = Set.empty
)

object ScalaOptions {
  implicit val hasHashData: HasHashData[ScalaOptions] = HasHashData.derive
  implicit val monoid: ConfigMonoid[ScalaOptions]     = ConfigMonoid.derive
}
