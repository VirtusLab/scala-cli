package scala.build.errors

import scala.build.errors.ScalaVersionError.getTheGeneralErrorInfo

final class InvalidBinaryScalaVersionError(
  val invalidBinaryVersion: String,
  val latestSupportedStableVersions: Seq[String]
) extends ScalaVersionError(s"Cannot find matching Scala version for '$invalidBinaryVersion'\n" +
      getTheGeneralErrorInfo(latestSupportedStableVersions))
