package scala.build.errors

final class UnsupportedScalaVersionError(
  val binaryVersion: String
) extends ScalaVersionError(s"Unsupported Scala version: $binaryVersion")
