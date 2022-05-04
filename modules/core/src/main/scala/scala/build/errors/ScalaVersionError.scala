package scala.build.errors

import scala.build.Position

class ScalaVersionError(message: String, positions: Seq[Position] = Nil, cause: Throwable = null)
    extends BuildException(message, positions = positions, cause)

object ScalaVersionError {
  def getTheGeneralErrorInfo(latestSupportedStableVersions: Seq[String]): String =
    s"""You can only choose one of the 3.x, 2.13.x, and 2.12.x. versions.
       |The latest supported stable versions are ${latestSupportedStableVersions.mkString(", ")}.
       |In addition, you can request compilation with the last nightly versions of Scala,
       |by passing the 2.nightly, 2.12.nightly, 2.13.nightly, or 3.nightly arguments.
       |Specific Scala 2 or Scala 3 nightly versions are also accepted.
       |""".stripMargin
}
