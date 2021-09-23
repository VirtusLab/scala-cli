package scala.build

import scala.collection.mutable.ListBuffer

object EitherSequence {
  def sequence[E, T](eithers: Seq[Either[E, T]]): Either[::[E], Seq[T]] = {
    val errors = new ListBuffer[E]
    val values = new ListBuffer[T]
    eithers.foreach {
      case Left(e) => errors += e
      case Right(t) =>
        if (errors.isEmpty)
          values += t
    }
    errors.result() match {
      case Nil    => Right(values.result())
      case h :: t => Left(::(h, t))
    }
  }
}
