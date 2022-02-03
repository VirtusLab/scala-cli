package scala.build.options.collections

import scala.build.Positioned

/** A collection of String options with arguments. Assumes either that: an option starts with '-'
  * and arguments to the option without it; an option starts with '-' and arguments are appended
  * with ':'; or option starts with @, where it is left as-is.
  */
class StringOptionsList(
  val underlyingMap: Map[String, Positioned[List[StringOptionsList.OptionArgument]]]
) {
  import StringOptionsList._
  private lazy val underlyingSeq: Seq[String] =
    underlyingMap.keys.toSeq.flatMap { key =>
      underlyingMap(key).value.foldLeft(List(key)) {
        case (list, Spaced(arg)) =>
          list :+ arg
        case (list, Coloned(arg)) =>
          list.tail :+ (list.head + arg)
        case (list, Prefixed(arg)) =>
          list.tail :+ (list.head + arg)
      }
    }

  def toSeq(): Seq[String] = underlyingSeq

  def orElse(other: StringOptionsList): StringOptionsList = {
    val newMap =
      (this.underlyingMap.keys.toList ++ other.underlyingMap.keys.toList).map { key =>
        key -> this.underlyingMap.getOrElse(key, other.underlyingMap(key))
      }.toMap
    new StringOptionsList(newMap)
  }

  def hashDataString(): String = {
    val sb = new StringBuilder()
    for ((k, v) <- underlyingMap.toVector.sortBy(_._1))
      sb.append(s"${k.hashCode()}:${v.hashCode()}\n")
    sb.toString()
  }

  override def toString(): String = {
    val sb = new StringBuilder()
    sb.append("StringOptionsList(")
    sb.append(underlyingSeq.mkString(", "))
    sb.append(")")

    sb.toString()
  }
}

object StringOptionsList {

  sealed trait OptionArgument
  case class Spaced(arg: String)   extends OptionArgument
  case class Coloned(arg: String)  extends OptionArgument
  case class Prefixed(arg: String) extends OptionArgument

  val empty = new StringOptionsList(Map.empty[String, Positioned[List[OptionArgument]]])

  def fromPositionedStringList(
    seq: Seq[Positioned[String]],
    optionPrefixes: Seq[String]
  ): StringOptionsList = {
    val underlyingMap =
      seq.foldLeft[(Option[String], Map[String, Positioned[List[OptionArgument]]])](
        None,
        Map.empty
      ) {
        case ((previousOption, previousMap), positioned) =>
          val positions = positioned.positions
          val element   = positioned.value

          val prefixMaybe = optionPrefixes.find(element.startsWith(_))

          val (currentOption, currentArguments) =
            if (prefixMaybe.isDefined) {
              val argument = element.substring(prefixMaybe.get.length())
              (Some(prefixMaybe.get), List(Prefixed(argument)))
            }
            else if (element.startsWith("-"))
              element.indexOf(":") match {
                case -1 => (Some(element), Nil)
                case idx =>
                  val (option, arg) = element.splitAt(idx)
                  (Some(option), List(Coloned(arg)))
              }
            else if (element.startsWith("@"))
              (Some(element), Nil)
            else {
              if (previousOption.isEmpty)
                throw new IllegalArgumentException(
                  "Option without an argument"
                ) // TODO reconsider throwing
              (
                previousOption,
                previousMap.getOrElse(
                  previousOption.get,
                  Positioned(positions, Nil)
                ).value :+ Spaced(element)
              )
            }

          val currentMap =
            previousMap + (currentOption.get -> Positioned(positions, currentArguments))

          (currentOption, currentMap)
      }._2

    new StringOptionsList(underlyingMap)
  }
}
