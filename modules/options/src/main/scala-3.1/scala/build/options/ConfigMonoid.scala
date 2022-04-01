package scala.build.options

import scala.deriving.*
import scala.compiletime.*
trait ConfigMonoid[T]:
  def zero: T
  def orElse(main: T, defaults: T): T
  def sum(values: Seq[T]): T =
    values.foldLeft(zero)(orElse(_, _))

case class ConfigMonoidImpl[T](override val zero: T)(orElseFun: (T, T) => T)
    extends ConfigMonoid[T]:
  def orElse(main: T, defaults: T) = orElseFun(main, defaults)

object ConfigMonoid:
  def apply[T](implicit instance: ConfigMonoid[T]): ConfigMonoid[T] = instance

  def instance[T](zeroValue: => T)(orElseFn: (T, T) => T): ConfigMonoid[T] =
    new ConfigMonoid[T] {
      def zero                         = zeroValue
      def orElse(main: T, defaults: T) = orElseFn(main, defaults)
    }

  def sum[T](values: Seq[T])(implicit monoid: ConfigMonoid[T]): T =
    monoid.sum(values)

  given seq[T]: ConfigMonoid[Seq[T]]       = ConfigMonoidImpl(Nil)(_ ++ _)
  given list[T]: ConfigMonoid[List[T]]     = ConfigMonoidImpl(Nil)(_ ++ _)
  given set[T]: ConfigMonoid[Set[T]]       = ConfigMonoidImpl(Set.empty)(_ ++ _)
  given option[T]: ConfigMonoid[Option[T]] = ConfigMonoidImpl(None)(_ orElse _)
  given boolean: ConfigMonoid[Boolean]     = ConfigMonoidImpl(false)(_ || _)
  given unit: ConfigMonoid[Unit]           = ConfigMonoidImpl[Unit](())((_, _) => ())
  given map[K, V](using valueMonoid: ConfigMonoid[V]): ConfigMonoid[Map[K, V]] =
    ConfigMonoidImpl(Map.empty[K, V]) {
      (main, defaults) =>
        (main.keySet ++ defaults.keySet).map {
          key =>
            val mainVal     = main.getOrElse(key, valueMonoid.zero)
            val defaultsVal = defaults.getOrElse(key, valueMonoid.zero)
            key -> valueMonoid.orElse(mainVal, defaultsVal)
        }.toMap
    }

  inline def zeroTuple[C <: Tuple]: Tuple =
    inline erasedValue[C] match
      case _: EmptyTuple => EmptyTuple
      case _: (t *: ts) =>
        summonInline[ConfigMonoid[t]].zero *: zeroTuple[ts]

  inline def valueTuple[C <: Tuple, T](index: Int, main: T, defaults: T): Tuple =
    inline erasedValue[C] match
      case _: EmptyTuple => EmptyTuple
      case _: (t *: ts) =>
        def get(v: T) = v.asInstanceOf[Product].productElement(index).asInstanceOf[t]
        summonInline[ConfigMonoid[t]].orElse(get(main), get(defaults)) *: valueTuple[ts, T](
          index + 1,
          main,
          defaults
        )

  inline given derive[T](using m: Mirror.ProductOf[T]): ConfigMonoid[T] =
    inline m match
      case p: Mirror.ProductOf[T] =>
        new ConfigMonoid[T]:
          def zero: T =
            p.fromProduct(zeroTuple[m.MirroredElemTypes])

          def orElse(main: T, defaults: T): T =
            p.fromProduct(valueTuple[m.MirroredElemTypes, T](0, main, defaults))
