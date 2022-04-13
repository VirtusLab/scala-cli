package scala.build

final case class EitherFailure[E](v: E, cps: EitherCps[_]) extends RuntimeException:
  override def fillInStackTrace() = this // disable stack trace generation

class EitherCps[E]

object EitherCps:
  def value[E, V](using
    cps: EitherCps[_ >: E]
  )(from: Either[E, V]) = // Adding a context bounds breaks incremental compilation
    from match
      case Left(e)  => throw EitherFailure(e, cps)
      case Right(v) => v

  final class Helper[E]():
    def apply[V](op: EitherCps[E] ?=> V): Either[E, V] =
      val cps = new EitherCps[E]
      try Right(op(using cps))
      catch
        case EitherFailure(e: E @unchecked, `cps`) =>
          Left(e)

  def either[E]: Helper[E] = new Helper[E]()
