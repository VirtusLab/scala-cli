package scala.build

import scala.build.options.{ConfigMonoid, HashedType, ShadowingSeq}
import scala.build.errors.BuildException
import scala.util.control.NonFatal
import scala.scalanative.build.Build

final case class Positioned[+T](
  positions: Seq[Position],
  value: T
) {
  def addPosition(position: Position*): Positioned[T] =
    copy(positions = this.positions ++ position)
  def addPositions(positions: Seq[Position]): Positioned[T] =
    copy(positions = this.positions ++ positions)

  def map[U](f: T => U): Positioned[U] =
    copy(value = f(value))
  def flatMap[U](f: T => Positioned[U]): Positioned[U] = {
    val pos0 = f(value)
    Positioned(
      positions ++ pos0.positions,
      pos0.value
    )
  }

  def mapEither[E, U](op: T => Either[E, U]): Either[E, Positioned[U]] =
    op(value).map(v => copy(value = v))

  def error(msg: String, cause: Throwable = null) = 
    Left(new BuildException(msg, positions, cause) {})

  def safeMap[V](op: T => V, message: String = "Exception") = 
    try Right(map(op)) catch 
      case NonFatal(e) => Left(new BuildException(s"$message: ${e.getMessage}", positions, e) {})

  
}

object Positioned {
  def apply[T](position: Position, value: T): Positioned[T] = Positioned(List(position), value)

  def none[T](value: T): Positioned[T] =
    Positioned(Nil, value)

  def commandLine[T](value: T): Positioned[T] =
    Positioned(List(Position.CommandLine()), value)

  def sequence[T](seq: Seq[Positioned[T]]): Positioned[Seq[T]] = {
    val allPositions = seq.flatMap(_.positions)
    val value        = seq.map(_.value)
    Positioned(allPositions, value)
  }

  implicit def hashedType[T](implicit underlying: HashedType[T]): HashedType[Positioned[T]] = {
    t =>
      underlying.hashedValue(t.value)
  }

  implicit def monoid[T](implicit underlying: ConfigMonoid[T]): ConfigMonoid[Positioned[T]] =
    ConfigMonoid.instance(Positioned.none(underlying.zero)) {
      (a, b) =>
        Positioned(a.positions ++ b.positions, underlying.orElse(a.value, b.value))
    }

  implicit def ordering[T](implicit underlying: Ordering[T]): Ordering[Positioned[T]] =
    Ordering.by(_.value)

  implicit def keyOf[T](implicit
    underlying: ShadowingSeq.KeyOf[T]
  ): ShadowingSeq.KeyOf[Positioned[T]] =
    ShadowingSeq.KeyOf(
      p => underlying.get(p.value),
      seq => underlying.groups(seq.map(_.value))
    )
}
