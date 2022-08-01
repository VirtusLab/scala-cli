package scala.build.errors

import scala.build.Position

abstract class BuildException(
  override val message: String,
  override val positions: Seq[Position] = Nil,
  cause: Throwable = null
) extends Exception(message, cause) with Diagnostic {
  final override def severity: Severity = Severity.Error

  /** @param default
    *   default value returned as a [[Right]] instance on recovery
    * @param maybeRecoverFunction
    *   potential recovery function, returns [[None]] on recovery and Some(identity(_)) otherwise
    * @tparam T
    *   type of the default value
    * @return
    *   Right(default) on recovery, Left(buildException) otherwise
    */
  def maybeRecoverWithDefault[T](
    default: T,
    maybeRecoverFunction: BuildException => Option[BuildException]
  ): Either[BuildException, T] = maybeRecoverFunction(this) match {
    case Some(e) => Left(e)
    case None    => Right(default)
  }
}
