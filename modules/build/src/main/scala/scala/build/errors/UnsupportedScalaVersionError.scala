package scala.build.errors

final class UnsupportedScalaVersionError(
  val binaryVersion: String,
  val supportedVersionsURL: String
) extends ScalaVersionError(s"Unsupported Scala version: $binaryVersion" + "\n" +
      s"You can choose one of the supported versions from the following URL: $supportedVersionsURL")
