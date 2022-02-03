package scala.build.options.collections

import scala.build.Positioned
import dependency.AnyDependency

object BuildOptionsConverterImplicits {
  implicit class StringOptionsListConverter(val seq: Seq[Positioned[String]]) extends AnyVal {

    /** Changes Seq[String] to OptionList, which allows for easier shadowing of repeated options.
      *
      * @return
      *   OptionList
      */
    def toStringOptionsList(directlyConnectedPrefixes: Seq[String]): StringOptionsList =
      StringOptionsList.fromPositionedStringList(seq, directlyConnectedPrefixes)
  }

  type DependencyMap = Map[String, Positioned[AnyDependency]]

  implicit class DependencyMapConverted(val internal: Seq[Positioned[AnyDependency]])
      extends AnyVal {

    /** Changes Seq[Positioned[AnyDependency]] to DependencyMap, which allows for easier shadowing
      * of repeated options.
      *
      * @return
      *   DependencyMap
      */
    def toDependencyMap(): DependencyMap =
      internal.map(posDep => posDep.value.module.render -> posDep).toMap
  }

}
