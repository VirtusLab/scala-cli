package scala.build.errors

import scala.build.Position

case class Diagnostic(
  message: String,
  severity: Severity,
  positions: Seq[Position] = Nil
)

abstract class BuildException(
  val message: String,
  val positions: Seq[Position] = Nil,
  cause: Throwable = null
) extends Exception(message, cause)
