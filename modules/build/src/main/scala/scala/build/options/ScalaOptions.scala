package scala.build.options

final case class ScalaOptions(
         scalaVersion: Option[String]  = None,
   scalaBinaryVersion: Option[String]  = None,
      addScalaLibrary: Option[Boolean] = None,
  generateSemanticDbs: Option[Boolean] = None,
        scalacOptions: Seq[String]     = Nil
) {
  def orElse(other: ScalaOptions): ScalaOptions =
    ScalaOptions(
      scalaVersion = scalaVersion.orElse(other.scalaVersion),
      scalaBinaryVersion = scalaBinaryVersion.orElse(other.scalaBinaryVersion),
      addScalaLibrary = addScalaLibrary.orElse(other.addScalaLibrary),
      generateSemanticDbs = generateSemanticDbs.orElse(other.generateSemanticDbs),
      scalacOptions = scalacOptions ++ other.scalacOptions
    )
}

object ScalaOptions {
  implicit val hasHashData: HasHashData[ScalaOptions] = HasHashData.derive
}
