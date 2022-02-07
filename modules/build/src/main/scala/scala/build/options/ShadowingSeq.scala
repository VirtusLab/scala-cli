package scala.build.options

import scala.build.Positioned
import dependency.AnyDependency
import scala.collection.mutable

/** Seq ensuring some of its values are unique according to some key */
final case class ShadowingSeq[T](values: Seq[T]) {
  def ++(other: Seq[T])(implicit key: ShadowingSeq.KeyOf[T]): ShadowingSeq[T] = {
    val l    = new mutable.ListBuffer[T]
    val seen = new mutable.HashSet[String]

    for (t <- (values ++ other)) {
      val keyOpt = key.get(t)
      if (!keyOpt.exists(seen.contains)) {
        l += t
        for (key <- keyOpt)
          seen += key
      }
    }

    ShadowingSeq(l.toSeq)
  }
}

object ShadowingSeq {
  final case class KeyOf[T](get: T => Option[String])
  implicit def monoid[T](implicit key: KeyOf[T]): ConfigMonoid[ShadowingSeq[T]] =
    ConfigMonoid.instance(ShadowingSeq.empty[T]) { (a, b) =>
      a ++ b.values
    }
  implicit def hashedType[T]: HashedType[ShadowingSeq[T]] = {
    a => a.toString
  }

  def empty[T]: ShadowingSeq[T] = ShadowingSeq(Nil)

  implicit val positionedAnyDependency: ShadowingSeq.KeyOf[Positioned[AnyDependency]] =
    ShadowingSeq.KeyOf(posDep => Some(posDep.value.module.render))
}

final case class JavaOpt(value: Seq[Positioned[String]]) {
  /* Hardcoded prefixes for java options */
  val optionPrefixes = Seq("-Xmn", "-Xms", "-Xmx", "-Xss")
  def key: Option[String] =
    if (!value.isEmpty) {
      val opt         = value(0).value
      val prefixMaybe = optionPrefixes.find(opt.startsWith(_))

      prefixMaybe.orElse {
        if (opt.startsWith("-"))
          Some(opt.split(':')(0))
        else if (opt.startsWith("@"))
          Some("@")
        else None
      }
    }
    else
      None
}
object JavaOpt {
  implicit val keyOf: ShadowingSeq.KeyOf[JavaOpt] =
    ShadowingSeq.KeyOf(_.key)

  def fromPositionedStringSeq(seq: Seq[Positioned[String]]): Seq[JavaOpt] =
    // println(OptUtils.groupCliOptions(seq).map(JavaOpt(_)))
    OptUtils.groupCliOptions(seq).map(JavaOpt(_))

  def toStringSeq(seq: Seq[JavaOpt]): Seq[String] =
    seq.flatMap(_.value).map(_.value)

  def toPositionedStringSeq(seq: Seq[JavaOpt]): Seq[Positioned[String]] =
    seq.flatMap(_.value)
}

final case class ScalacOpt(value: Seq[Positioned[String]]) {
  val repeatingKeys = Set("-Xplugin:")
  def key: Option[String] =
    if (!value.isEmpty) {
      val opt = value(0).value

      val key =
        if (opt.startsWith("-"))
          Some(opt.split(':')(0))
        else if (opt.startsWith("@"))
          Some("@")
        else None

      if (key.exists(repeatingKeys.contains)) None
      else key
    }
    else
      None
}
object ScalacOpt {
  implicit val keyOf: ShadowingSeq.KeyOf[ScalacOpt] =
    ShadowingSeq.KeyOf(_.key)

  def fromPositionedStringSeq(seq: Seq[Positioned[String]]): Seq[ScalacOpt] =
    OptUtils.groupCliOptions(seq).map(ScalacOpt(_))

  def toStringSeq(seq: Seq[ScalacOpt]): Seq[String] =
    seq.flatMap(_.value).map(_.value)

  def toPositionedStringSeq(seq: Seq[ScalacOpt]): Seq[Positioned[String]] =
    seq.flatMap(_.value)
}

object OptUtils {
  // Groups options (starting with `-` or `@`) with option arguments that follow
  def groupCliOptions(opts: Seq[Positioned[String]]): Seq[Seq[Positioned[String]]] = {
    val seqListBuffer = new mutable.ListBuffer[Seq[Positioned[String]]]
    val optListBuffer = new mutable.ListBuffer[Positioned[String]]
    for (element <- opts) {
      val opt = element.value
      if (opt.startsWith("-") || opt.startsWith("@"))
        if (!optListBuffer.isEmpty) {
          seqListBuffer += optListBuffer.toSeq
          optListBuffer.clear()
        }
      optListBuffer += element
    }
    seqListBuffer += optListBuffer.toSeq

    seqListBuffer.toSeq
  }
}
