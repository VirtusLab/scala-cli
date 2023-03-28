package scala.build

import scala.collection.mutable.ListBuffer

object Ops {
  implicit class EitherSeqOps[E, T](private val seq: Seq[Either[E, T]]) extends AnyVal {
    def sequence: Either[::[E], Seq[T]] =
      EitherSequence.sequence(seq)
  }

  implicit class EitherIteratorOps[E, T](private val it: Iterator[Either[E, T]]) extends AnyVal {
    def sequence0: Either[E, Seq[T]] = {
      val b      = new ListBuffer[T]
      var errOpt = Option.empty[E]
      while (it.hasNext && errOpt.isEmpty) {
        val e = it.next()
        e match {
          case Left(err) =>
            errOpt = Some(err)
          case Right(t) =>
            b += t
        }
      }
      errOpt.toLeft(b.result())
    }
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

  implicit class EitherMap2[Ex <: Throwable, ExA <: Ex, ExB <: Ex, A, B](
    private val eithers: (Either[ExA, A], Either[ExB, B])
  ) extends AnyVal {
    def traverseN: Either[::[Ex], (A, B)] =
      eithers match {
        case (Right(a), Right(b)) => Right((a, b))
        case _ =>
          val errors = eithers._1.left.toOption.toSeq ++
            eithers._2.left.toOption.toSeq
          val errors0 = errors.toList match {
            case Nil    => sys.error("Cannot happen")
            case h :: t => ::(h, t)
          }
          Left(errors0)
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

  implicit class EitherMap4[
    Ex <: Throwable,
    ExA <: Ex,
    ExB <: Ex,
    ExC <: Ex,
    ExD <: Ex,
    A,
    B,
    C,
    D
  ](
    private val eithers: (Either[ExA, A], Either[ExB, B], Either[ExC, C], Either[ExD, D])
  ) extends AnyVal {
    def traverseN: Either[::[Ex], (A, B, C, D)] =
      eithers match {
        case (Right(a), Right(b), Right(c), Right(d)) => Right((a, b, c, d))
        case _ =>
          val errors = eithers._1.left.toOption.toSeq ++
            eithers._2.left.toOption.toSeq ++
            eithers._3.left.toOption.toSeq ++
            eithers._4.left.toOption.toSeq
          val errors0 = errors.toList match {
            case Nil    => sys.error("Cannot happen")
            case h :: t => ::(h, t)
          }
          Left(errors0)
      }
  }

  implicit class EitherMap5[
    Ex <: Throwable,
    ExA <: Ex,
    ExB <: Ex,
    ExC <: Ex,
    ExD <: Ex,
    ExE <: Ex,
    A,
    B,
    C,
    D,
    E
  ](
    private val eithers: (
      Either[ExA, A],
      Either[ExB, B],
      Either[ExC, C],
      Either[ExD, D],
      Either[ExE, E]
    )
  ) extends AnyVal {
    def traverseN: Either[::[Ex], (A, B, C, D, E)] =
      eithers match {
        case (Right(a), Right(b), Right(c), Right(d), Right(e)) => Right((a, b, c, d, e))
        case _ =>
          val errors = eithers._1.left.toOption.toSeq ++
            eithers._2.left.toOption.toSeq ++
            eithers._3.left.toOption.toSeq ++
            eithers._4.left.toOption.toSeq ++
            eithers._5.left.toOption.toSeq
          val errors0 = errors.toList match {
            case Nil    => sys.error("Cannot happen")
            case h :: t => ::(h, t)
          }
          Left(errors0)
      }
  }

  implicit class EitherMap6[
    Ex <: Throwable,
    ExA <: Ex,
    ExB <: Ex,
    ExC <: Ex,
    ExD <: Ex,
    ExE <: Ex,
    ExF <: Ex,
    A,
    B,
    C,
    D,
    E,
    F
  ](
    private val eithers: (
      Either[ExA, A],
      Either[ExB, B],
      Either[ExC, C],
      Either[ExD, D],
      Either[ExE, E],
      Either[ExF, F]
    )
  ) extends AnyVal {
    def traverseN: Either[::[Ex], (A, B, C, D, E, F)] =
      eithers match {
        case (Right(a), Right(b), Right(c), Right(d), Right(e), Right(f)) =>
          Right((a, b, c, d, e, f))
        case _ =>
          val errors = eithers._1.left.toOption.toSeq ++
            eithers._2.left.toOption.toSeq ++
            eithers._3.left.toOption.toSeq ++
            eithers._4.left.toOption.toSeq ++
            eithers._5.left.toOption.toSeq ++
            eithers._6.left.toOption.toSeq
          val errors0 = errors.toList match {
            case Nil    => sys.error("Cannot happen")
            case h :: t => ::(h, t)
          }
          Left(errors0)
      }
  }
}
