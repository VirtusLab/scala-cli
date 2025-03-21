package scala.build

import scala.util.matching.Regex

final case class Named[+T](
  name: Option[String],
  value: T
) {
  def map[U](f: T => U): Named[U] =
    copy(value = f(value))

  def entry: Option[(String, T)] =
    name.map(n => n -> value)
}

object Named {
  private val NameRegex: Regex = "^\\[\\w+\\]$".r

  def apply[T](name: String, value: T): Named[T] = Named(Some(name), value)

  def none[T](value: T): Named[T] =
    Named(None, value)

  given [T]: Conversion[Named[T], IterableOnce[(String, T)]] =
    named => named.entry

  def fromKey(key: String): Named[String] = {
    val parts = key.split('.')
    val name  = parts.find(NameRegex.matches)
    val rest  = parts.filterNot(name.contains)

    Named(name, rest.mkString("."))
  }
}
