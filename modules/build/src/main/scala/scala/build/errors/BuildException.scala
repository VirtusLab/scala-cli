package scala.build.errors

import scala.build.Position

abstract class BuildException(
  val message: String,
  val positions: Seq[Position] = Nil,
  cause: Throwable = null
) extends Exception(message, cause)
