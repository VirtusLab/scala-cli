package scala.build.tastylib

import scala.build.tastylib.internal.Constants
import scala.util.Try

object TastyVersions {
  implicit class VersionOps(version: String) {
    def majorVersion: Int               = version.split('.')(0).toInt
    def minorVersion: Int               = version.split('.')(1).toInt
    def minorVersionOption: Option[Int] = Try(minorVersion).toOption
  }

  // Every time tasty version is updated, please update LatestSupportedScala as well!
  private object LatestSupportedScala {
    final val MajorVersion: Int = Constants.latestSupportedScala.majorVersion
    final val MinorVersion: Int = Constants.latestSupportedScala.minorVersion

    def isLatestSupportedMajorVersion(scalaVersion: String): Boolean =
      scalaVersion.startsWith(s"${LatestSupportedScala.MajorVersion}.") ||
      scalaVersion == LatestSupportedScala.MajorVersion.toString
  }

  def shouldRunPreprocessor(
    scalaVersion: String,
    scalaCliVersion: String
  ): Either[String, Boolean] =
    if (!LatestSupportedScala.isLatestSupportedMajorVersion(scalaVersion)) Right(false)
    else scalaVersion.minorVersionOption match {
      case Some(scalaMinor) if scalaMinor > LatestSupportedScala.MinorVersion =>
        Left(
          s"""Scala CLI (v$scalaCliVersion) cannot post process TASTY files from Scala $scalaVersion.
             |This is not a fatal error since post processing only cleans up source paths in TASTY file.
             |It should not affect your application.
             |You may be getting this warning because you are using a newer version of Scala than the one supported by Scala CLI (v$scalaCliVersion).
             |Make sure your Scala CLI is up-to-date.
             |You may need to wait for $scalaVersion support in a future version of Scala CLI.
             |""".stripMargin
        )
      case _ => Right(true)
    }
}
