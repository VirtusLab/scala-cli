package scala.build.errors

final class InvalidBinaryScalaVersionError(
  val binaryVersion: String
) extends ScalaVersionError(s"Cannot find matching Scala version for '$binaryVersion'")
