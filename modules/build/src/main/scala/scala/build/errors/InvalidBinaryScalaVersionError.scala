package scala.build.errors

final class InvalidBinaryScalaVersionError(
  val binaryVersion: String
) extends BuildException(s"Cannot find matching Scala version for '$binaryVersion'")
