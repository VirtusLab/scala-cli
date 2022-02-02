package scala.build.options.collections

// TODO add hardcoded beginnings for scalac and java options (especially -Xmx<size> -Xms<size>)

/** A collection of String options with arguments. Assumes either that: an option starts with '-' and
  * arguments to the option without it; an option starts with '-' and arguments are appended with
  * ':'; or option starts with @, where it is left as-is.
  */
class StringOptionsList(
  val underlyingMap: Map[String, List[StringOptionsList.OptionArgument]],
  private val prefixedOptions: Seq[String] = Seq.empty
) {
  private lazy val underlyingSeq =
    underlyingMap.keys.toList.flatMap { key =>
      underlyingMap(key).foldLeft(List(key)) {
        case (list, StringOptionsList.Spaced(arg)) =>
          list :+ arg
        case (list, StringOptionsList.Coloned(arg)) =>
          list.tail :+ (list.head + arg)
      }
    }

  def toSeq(): List[String] = underlyingSeq

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
  case class Spaced(arg: String) extends OptionArgument
  case class Coloned(arg: String) extends OptionArgument

  val empty = new StringOptionsList(Map.empty[String, List[OptionArgument]])

  def fromStringList(list: Seq[String]): StringOptionsList = {
    val underlyingMap =
      list.foldLeft[(Option[String], Map[String, List[OptionArgument]])](None, Map.empty) {
        case ((previousOption, previousMap), element) =>
          val (currentOption, currentArguments) =
            if (element.startsWith("-")) {
              element.indexOf(":") match {
                case -1    => (Some(element), Nil)
                case idx => 
                  val (option, arg) = element.splitAt(idx)
                  (Some(option), List(Coloned(arg)))
              }
            } else if (element.startsWith("@")) {
              (Some(element), Nil)
            } else {
              if (previousOption.isEmpty)
                throw new IllegalArgumentException("Option without an argument") // TODO reconsider throwing
              (previousOption, previousMap.getOrElse(previousOption.get, Nil) :+ Spaced(element))
            }

          val currentMap = previousMap + (currentOption.get -> currentArguments)
          (currentOption, currentMap)
      }._2

    new StringOptionsList(underlyingMap)
  }
}
