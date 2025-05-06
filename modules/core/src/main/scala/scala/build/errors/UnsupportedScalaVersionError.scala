package scala.build.errorsimport
scala.build.errors.ScalaVersionError.getTheGeneralErrorInfo

final class UnsupportedScalaVersionError(val binaryVersion: String)
    extends ScalaVersionError(s"Unsupported Scala version: $binaryVersion")
