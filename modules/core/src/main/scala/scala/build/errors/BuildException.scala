package scala.build.errors

import scala.build.Position

abstract class BuildException(
  override val message: String,
  override val positions: Seq[Position] = Nil,
  cause: Throwable = null
) extends Exception(message, cause) with Diagnostic {
  final override def severity: Severity = Severity.Error
}

object BuildException {
  implicit class EitherOps[T](seq: Seq[Either[BuildException, T]]) {
    import scala.build.Ops._
    def sequenceToComposite = seq.sequence.left.map(CompositeBuildException.apply)
  }
}
