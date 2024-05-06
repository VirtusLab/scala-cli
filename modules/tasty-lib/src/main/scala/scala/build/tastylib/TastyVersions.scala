package scala.build.tastylib

import scala.build.tastylib.internal.Constants
import scala.util.Try

object TastyVersions {
  implicit class VersionOps(version: String) {
    def majorVersion: Int               = version.split('.')(0).toInt
    def minorVersion: Int               = version.split('.')(1).toInt
    def minorVersionOption: Option[Int] = Try(minorVersion).toOption
    def isLatestSupportedMajorVersion(latestSupportedScalaVersion: String): Boolean = {
      val latestSupportedMajor = latestSupportedScalaVersion.majorVersion.toString
      version.startsWith(s"$latestSupportedMajor.") || version == latestSupportedMajor
    }
  }

  def shouldRunPreprocessor(
    scalaVersion: String,
    scalaCliVersion: String,
    defaultScalaVersion: Option[String]
  ): Either[String, Boolean] = {
    val scalaDefault = defaultScalaVersion.getOrElse(Constants.defaultScalaVersion)
    if (!scalaVersion.isLatestSupportedMajorVersion(scalaDefault)) Right(false)
    else scalaVersion.minorVersionOption match {
      case Some(scalaMinor) if scalaMinor > scalaDefault.minorVersion =>
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
}
