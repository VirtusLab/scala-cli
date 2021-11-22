package scala.build.options

import shapeless._

trait HashedField[T] {
  def add(name: String, t: T, update: String => Unit): Unit
}

object HashedField extends LowPriorityHashedField {
  def apply[T](implicit instance: HashedField[T]): HashedField[T] = instance

  implicit def asIs[T](implicit hasher: Lazy[HashedType[T]]): HashedField[T] = {
    (name, t, update) =>
      update(s"$name=${hasher.value.hashedValue(t)}")
  }

  implicit def seq[T](implicit hasher: Lazy[HashedType[T]]): HashedField[Seq[T]] = {
    (name, seq, update) =>
      for (t <- seq)
        update(s"$name+=${hasher.value.hashedValue(t)}")
  }

  implicit def list[T](implicit hasher: Lazy[HashedType[T]]): HashedField[List[T]] = {
    (name, list, update) =>
      for (t <- list)
        update(s"$name+=${hasher.value.hashedValue(t)}")
  }

  implicit def option[T](implicit hasher: Lazy[HashedType[T]]): HashedField[Option[T]] = {
    (name, opt, update) =>
      for (t <- opt)
        update(s"$name=${hasher.value.hashedValue(t)}")
  }

  implicit def set[T](implicit
    hasher: Lazy[HashedType[T]],
    ordering: Ordering[T]
  ): HashedField[Set[T]] = {
    (name, set, update) =>
      for (t <- set.toVector.sorted(ordering))
        update(s"$name+=${hasher.value.hashedValue(t)}")
  }

  implicit def map[K, V](
    implicit
    hasherK: Lazy[HashedType[K]],
    hasherV: Lazy[HashedType[V]],
    ordering: Ordering[K]
  ): HashedField[Map[K, V]] = {
    (name, map0, update) =>
      for (t <- map0.keys.toVector.sorted(ordering))
        update(s"$name+=${hasherK.value.hashedValue(t) + hasherV.value.hashedValue(map0(t))}")
  }

}

abstract class LowPriorityHashedField {

  implicit def recurse[T](implicit hasher: HasHashData[T]): HashedField[T] = {
    (name, t, update) =>
      hasher.add(name + ".", t, update)
  }

}
