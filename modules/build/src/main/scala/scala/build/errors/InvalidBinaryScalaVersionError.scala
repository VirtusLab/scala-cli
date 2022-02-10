package scala.build.errors

final class InvalidBinaryScalaVersionError(
  val invalidBinaryVersion: String,
  val supportedVersionsURL: String
) extends ScalaVersionError(s"Cannot find matching Scala version for '$invalidBinaryVersion'\n" +
      s"You can choose one of the supported versions from the following URL: $supportedVersionsURL")
