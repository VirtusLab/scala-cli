package scala.build.errorsimport
scala.build.errors.ScalaVersionError.getTheGeneralErrorInfo

final class InvalidBinaryScalaVersionError(val invalidBinaryVersion: String)
    extends ScalaVersionError(s"Cannot find matching Scala version for '$invalidBinaryVersion'")
