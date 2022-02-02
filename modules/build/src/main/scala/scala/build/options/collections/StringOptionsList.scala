package scala.build.options.collections

import scala.build.Positioned
import dependency.AnyDependency

// TODO add hardcoded beginnings for scalac and java options (especially -Xmx<size> -Xms<size>)

// TODO reconsider extending map
// consider @ (used in scalac and javac)
/** 
 *  A collection of String options with arguments.
 *  Assumes either that:
 *   * option starts with '-' and arguments to the option without it;
 *   * option starts with '-' and arguments are appended with ':'. TODO
 *   * option starts with @, where it is left as-is. TODO
 */
class StringOptionsList(val underlyingMap: Map[String, List[String]]) {
  private lazy val underlyingSeq =
    underlyingMap.keys.toList.flatMap(key => key +: underlyingMap(key))

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
}

object StringOptionsList {
  val empty = new StringOptionsList(Map.empty[String, List[String]]) 
}

object StringOptionsListConversionImplicits { // BuildOptionsConverterImplicits - better name
  implicit class StringOptionsListConverter(val internal: Seq[String]) extends AnyVal {
    /**
      * Changes Seq[String] to OptionList, which allows for easier shadowing of repeated options.
      *
      * @return OptionList
      */
    def toStringOptionList(): StringOptionsList = {
      val underlyingMap =
        internal.foldLeft[(Option[String], Map[String, List[String]])](None, Map.empty) {
          case ((previousOption, previousMap), element) =>
            val (currentOption, currentArguments) =
              if (element.startsWith("-")) {
                (Some(element), Nil)
              } else {
                if (previousOption.isEmpty) throw new IllegalArgumentException("Value without an argument")
                (previousOption, previousMap.getOrElse(previousOption.get, Nil) :+ element)
              }
            
            val currentMap = previousMap + (currentOption.get -> currentArguments)
            (currentOption, currentMap)
        }._2

      new StringOptionsList(underlyingMap)
    }
  }

  type DependencyMap = Map[String, Positioned[AnyDependency]]

  implicit class DependencyMapConverted(val internal: Seq[Positioned[AnyDependency]]) extends AnyVal {
    /** Changes Seq[Positioned[AnyDependency]] to DependencyMap, which allows for easier shadowing of repeated options.
      *
      * @return DependencyMap
      */
    def toDependencyMap(): DependencyMap =
      internal.map(posDep => posDep.value.module.render -> posDep).toMap
  }

}
