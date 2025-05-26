package scala.build.errors

final class InvalidBinaryScalaVersionError(val invalidBinaryVersion: String)
    extends ScalaVersionError(s"Cannot find matching Scala version for '$invalidBinaryVersion'")
