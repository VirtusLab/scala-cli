package scala.build.options

import scala.compiletime.*
import scala.deriving.*

trait HasHashData[T]:
  def add(prefix: String, t: T, update: String => Unit): Unit

object HasHashData:
  def nop[T]: HasHashData[T] = (_, _, _) => ()

  inline def doAdd[C <: Tuple, T](
    index: Int,
    main: T,
    prefix: String,
    indexes: Seq[String],
    update: String => Unit
  ): Unit =
    inline erasedValue[C] match
      case _: EmptyTuple => ()
      case _: (t *: ts) =>
        val hasher     = summonInline[HasHashData[t]]
        val newPrefix  = prefix + "." + indexes(index) // in 2.x we were not adding '.'
        val childValue = main.asInstanceOf[Product].productElement(index).asInstanceOf[t]
        hasher.add(newPrefix, childValue, update)

        doAdd[ts, T](index + 1, main, prefix, indexes, update)

  inline given derive[T](using m: Mirror.ProductOf[T]): HasHashData[T] =
    inline m match
      case p: Mirror.ProductOf[T] =>
        new HasHashData[T]:
          def add(prefix: String, main: T, update: String => Unit): Unit =
            val labels =
              constValueTuple[p.MirroredElemLabels].productIterator.toList.map(_.toString)
            doAdd[m.MirroredElemTypes, T](0, main, prefix, labels, update)

  given asIs[T](using hasher: HashedType[T]): HasHashData[T] =
    (prefix, t, update) => update(s"$prefix=${hasher.hashedValue(t)}")

  given seq[T](using hasher: HashedType[T]): HasHashData[Seq[T]] = // shouldn't we hash index here?
    (name, seq, update) => seq.foreach(t => update(s"$name+=${hasher.hashedValue(t)}"))

  given list[T](using
    hasher: HashedType[T]
  ): HasHashData[List[T]] = // shouldn't we hash index here?
    (name, list, update) => list.foreach(t => update(s"$name+=${hasher.hashedValue(t)}"))

  given option[T](using hasher: HashedType[T]): HasHashData[Option[T]] =
    (name, opt, update) => opt.foreach(t => update(s"$name=${hasher.hashedValue(t)}"))

  given set[T](using hasher: HashedType[T], ordering: Ordering[T]): HasHashData[Set[T]] =
    (name, opt, update) =>
      opt.toVector.sorted(ordering)
        .foreach(t => update(s"$name=${hasher.hashedValue(t)}"))

  given map[K, V](using
    hasherK: HashedType[K],
    hasherV: HashedType[V],
    ordering: Ordering[K]
  ): HasHashData[Map[K, V]] =
    (name, map0, update) =>
      for ((k, v) <- map0.toVector.sortBy(_._1)(ordering))
        update(
          s"$name+=${hasherK.hashedValue(k)}${hasherV.hashedValue(v)}"
        ) // should't we seperate key and value here?
