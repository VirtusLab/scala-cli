package scala.build.errors

import scala.build.Position

class ScalaVersionError(message: String, positions: Seq[Position] = Nil)
    extends BuildException(message, positions = positions)

object ScalaVersionError {
  def getTheGeneralErrorInfo(latestSupportedStableVersions: Seq[String]): String =
    s"""You can only choose one of the 3.x, 2.13.x, and 2.12.x. versions.
       |The latest supported stable versions are ${latestSupportedStableVersions.mkString(", ")}.
       |In addition, you can request the latest Scala 2 and Scala 3 nightly versions by passing 2.nightly, and 3.nightly arguments respectively.
       |For requesting a specific Scala 2 or Scala 3 nightly version, please specify the full version of the nightly without the repository argument.
       |""".stripMargin
}
