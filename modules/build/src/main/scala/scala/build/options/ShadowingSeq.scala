package scala.build.options

import dependency.AnyDependency

import scala.collection.mutable

/** Seq ensuring some of its values are unique according to some key */
final case class ShadowingSeq[T] private (values: Seq[Seq[T]]) {
  lazy val toSeq: Seq[T] = values.flatten
  def ++(other: Seq[T])(implicit key: ShadowingSeq.KeyOf[T]): ShadowingSeq[T] =
    addGroups(ShadowingSeq.groups(other, key.groups(other)))
  private def addGroups(other: Seq[Seq[T]])(implicit key: ShadowingSeq.KeyOf[T]): ShadowingSeq[T] =
    if (other.isEmpty) this
    else {
      val l    = new mutable.ListBuffer[Seq[T]]
      val seen = new mutable.HashSet[String]

      for (group <- values.iterator ++ other.iterator) {
        assert(group.nonEmpty)
        val keyOpt = key.get(group.head)
        if (!keyOpt.exists(seen.contains)) {
          l += group
          for (key <- keyOpt)
            seen += key
        }
      }

      ShadowingSeq(l.toList)
    }
}

object ShadowingSeq {

  final case class KeyOf[T](
    get: T => Option[String],
    /** The indices at which sub-groups of values start */
    groups: Seq[T] => Seq[Int]
  )
  object KeyOf {
    implicit val keyOfAnyDependency: KeyOf[AnyDependency] =
      KeyOf(dep => Some(dep.module.render), _.indices)
  }

  implicit def monoid[T](implicit key: KeyOf[T]): ConfigMonoid[ShadowingSeq[T]] =
    ConfigMonoid.instance(ShadowingSeq.empty[T]) { (a, b) =>
      a.addGroups(b.values)
    }
  implicit def hashedType[T]: HashedType[ShadowingSeq[T]] = {
    a => a.toString
  }

  def empty[T]: ShadowingSeq[T] = ShadowingSeq(Nil)

  def from[T](values: Seq[T])(implicit key: KeyOf[T]): ShadowingSeq[T] =
    empty[T] ++ values

  private def groups[T](values: Seq[T], indices: Seq[Int]): Seq[Seq[T]] = {
    val safeIndices = Seq(0) ++ indices ++ Seq(values.length)
    safeIndices
      .sliding(2)
      .map {
        case Seq(start, end) =>
          values.slice(start, end)
      }
      .filter(_.nonEmpty)
      .toVector
  }
}
