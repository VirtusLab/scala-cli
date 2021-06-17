package scala.build.options

final case class ScalaOptions(
        scalaVersion: Option[String]  = None,
  scalaBinaryVersion: Option[String]  = None,
     addScalaLibrary: Option[Boolean] = None
) {
  def addHashData(update: String => Unit): Unit = {
    for (sv <- scalaVersion)
      update("scalaVersion=" + sv + "\n")
    for (sbv <- scalaBinaryVersion)
      update("scalaBinaryVersion=" + sbv + "\n")
    for (add <- addScalaLibrary)
      update("addScalaLibrary=" + add.toString + "\n")
  }

  def orElse(other: ScalaOptions): ScalaOptions =
    ScalaOptions(
      scalaVersion = scalaVersion.orElse(scalaVersion),
      scalaBinaryVersion = scalaBinaryVersion.orElse(scalaBinaryVersion),
      addScalaLibrary = addScalaLibrary.orElse(addScalaLibrary)
    )
}
