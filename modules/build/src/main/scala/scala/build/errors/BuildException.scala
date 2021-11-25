package scala.build.errors

import scala.build.Position

case class Diagnostic(
  val message: String,
  val positions: Seq[Position] = Nil,
  val severity: Severity
)

abstract class BuildException(
  val message: String,
  val positions: Seq[Position] = Nil,
  cause: Throwable = null
) extends Exception(message, cause)
