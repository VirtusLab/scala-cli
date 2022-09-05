package scala.build.tastylib

import scala.build.tastylib.internal.Constants

object TastyVersions {

  // Every time tasty version is updated, please update LatestSupportedScala as well!
  object LatestSupportedScala {
    final val MajorVersion: Int = 3
    final val MinorVersion: Int = Constants.latestSupportedScala.split('.')(1).toInt
  }

  def shouldRunPreprocessor(
    scalaVersion: String,
    scalaCliVersion: String
  ): Either[String, Boolean] =
    if (!scalaVersion.startsWith("3.") && scalaVersion != "3") Right(false)
    else
      scalaVersion.split('.')(1).toInt match {
        case scalaMinor if scalaMinor > LatestSupportedScala.MinorVersion =>
          Left(
            s"Scala CLI (v. $scalaCliVersion) cannot post process TASTY files from Scala $scalaVersion.\n" +
              s"This is not a fatal error since post processing only cleans up source paths in TASTY file " +
              s"and it should not affect your application.\n" +
              s"To get rid of this message, please update Scala CLI version."
          )
        case _ =>
          Right(true)
      }
}
