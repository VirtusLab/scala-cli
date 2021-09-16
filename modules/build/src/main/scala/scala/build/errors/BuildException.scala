package scala.build.errors

abstract class BuildException(
  val message: String,
  cause: Throwable = null
) extends Exception(message, cause)
