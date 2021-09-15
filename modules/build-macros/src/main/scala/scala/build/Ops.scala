package scala.build

object Ops {
  implicit class EitherSeqOps[E, T](private val seq: Seq[Either[E, T]]) extends AnyVal {
    def traverse: Either[::[E], Seq[T]] =
      EitherTraverse.traverse(seq)
  }

  implicit class EitherThrowOps[E <: Throwable, T](private val either: Either[E, T])
      extends AnyVal {
    def orThrow: T =
      either match {
        case Left(e)  => throw new Exception(e)
        case Right(t) => t
      }
  }
}
