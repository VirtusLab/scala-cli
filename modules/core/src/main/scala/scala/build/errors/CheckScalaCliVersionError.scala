package scala.build.errors

class CheckScalaCliVersionError(val message: String, val cause: Throwable)
    extends Exception(message, cause)

object CheckScalaCliVersionError {
  def apply(message: String, cause: Throwable = null): CheckScalaCliVersionError =
    new CheckScalaCliVersionError(message, cause)
}
