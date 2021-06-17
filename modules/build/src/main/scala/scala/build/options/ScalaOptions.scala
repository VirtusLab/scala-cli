package scala.build.options

final case class ScalaOptions(
         scalaVersion: Option[String]  = None,
   scalaBinaryVersion: Option[String]  = None,
      addScalaLibrary: Option[Boolean] = None,
  generateSemanticDbs: Option[Boolean] = None,
        scalacOptions: Seq[String]     = Nil
) {
  def addHashData(update: String => Unit): Unit = {
    for (sv <- scalaVersion)
      update("scalaVersion=" + sv + "\n")
    for (sbv <- scalaBinaryVersion)
      update("scalaBinaryVersion=" + sbv + "\n")
    for (add <- addScalaLibrary)
      update("addScalaLibrary=" + add.toString + "\n")
    for (generate <- generateSemanticDbs)
      update("generateSemanticDbs=" + generate.toString + "\n")
    for (opt <- scalacOptions)
      update("scalacOptions+=" + opt + "\n")
  }

  def orElse(other: ScalaOptions): ScalaOptions =
    ScalaOptions(
      scalaVersion = scalaVersion.orElse(other.scalaVersion),
      scalaBinaryVersion = scalaBinaryVersion.orElse(other.scalaBinaryVersion),
      addScalaLibrary = addScalaLibrary.orElse(other.addScalaLibrary),
      generateSemanticDbs = generateSemanticDbs.orElse(other.generateSemanticDbs),
      scalacOptions = scalacOptions ++ other.scalacOptions
    )
}
