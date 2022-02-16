package scala.build.errors

import scala.build.errors.ScalaVersionError.getTheGeneralErrorInfo

final class UnsupportedScalaVersionError(
  val binaryVersion: String,
  val latestSupportedStableVersions: Seq[String]
) extends ScalaVersionError(
      s"Unsupported Scala version: $binaryVersion" + "\n" + getTheGeneralErrorInfo(
        latestSupportedStableVersions
      )
    )
