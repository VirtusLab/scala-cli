package scala.build.errors

import scala.build.errors.ScalaVersionError.getTheGeneralErrorInfo

final class NoValidScalaVersionFoundError(
  val foundVersions: Seq[String],
  val latestSupportedStableVersions: Seq[String]
) extends ScalaVersionError(
      s"Cannot find a valid matching Scala version among ${foundVersions.mkString(", ")}\n" +
        getTheGeneralErrorInfo(latestSupportedStableVersions)
    )
