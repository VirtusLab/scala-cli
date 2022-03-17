package scala.build.errors

final class ScalaNativeCompatibilityError(
  val scalaVersion: String,
  val scalaNativeVersion: String
) extends BuildException(
      s"""Used Scala Native version $scalaNativeVersion is incompatible with Scala $scalaVersion.
         |Please try one of the following combinations:
         |  Scala Native version >= 0.4.4 for Scala 3.1 (*.sc & *.scala files)
         |  Scala Native version >= 0.4.0 for Scala 2.13 (*.sc & *.scala files)
         |  Scala Native version >= 0.4.0 for Scala 2.12 (*.scala files)
         |Windows is supported since Scala Native 0.4.1.
         |""".stripMargin
    )
