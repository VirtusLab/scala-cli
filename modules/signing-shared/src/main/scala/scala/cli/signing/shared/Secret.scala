package scala.cli.signing.shared

final class Secret[+T](
  value0: T
) {
  def value: T = value0

  override def equals(obj: Any): Boolean =
    obj match {
      case other: Secret[_] => value == other.value
      case _                => false
    }

  // not leaking details about the secret here
  override def hashCode(): Int  = 0
  override def toString: String = "****"
}

object Secret {
  def apply[T](value: T): Secret[T] =
    new Secret(value)
}
