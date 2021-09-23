package scala.build

object Ops {
  implicit class EitherSeqOps[E, T](private val seq: Seq[Either[E, T]]) extends AnyVal {
    def sequence: Either[::[E], Seq[T]] =
      EitherSequence.sequence(seq)
  }

  implicit class EitherOptOps[E, T](private val opt: Option[Either[E, T]]) extends AnyVal {
    def sequence: Either[E, Option[T]] =
      opt match {
        case None           => Right(None)
        case Some(Left(e))  => Left(e)
        case Some(Right(t)) => Right(Some(t))
      }
  }

  implicit class EitherThrowOps[E <: Throwable, T](private val either: Either[E, T])
      extends AnyVal {
    def orThrow: T =
      either match {
        case Left(e)  => throw new Exception(e)
        case Right(t) => t
      }
  }

  implicit class EitherMap3[Ex <: Throwable, ExA <: Ex, ExB <: Ex, ExC <: Ex, A, B, C](
    private val eithers: (Either[ExA, A], Either[ExB, B], Either[ExC, C])
  ) extends AnyVal {
    def traverseN: Either[::[Ex], (A, B, C)] =
      eithers match {
        case (Right(a), Right(b), Right(c)) => Right((a, b, c))
        case _ =>
          val errors = eithers._1.left.toOption.toSeq ++
            eithers._2.left.toOption.toSeq ++
            eithers._3.left.toOption.toSeq
          val errors0 = errors.toList match {
            case Nil    => sys.error("Cannot happen")
            case h :: t => ::(h, t)
          }
          Left(errors0)
      }
  }
}
