package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.Value

import scala.jdk.CollectionConverters._

object DirectiveUtil {
  def stringValues(values: Seq[Any]): Seq[String] =
    values
      .collect {
        case list: java.util.List[_] =>
          list
            .asScala
            .map {
              case v: Value[_] => v.get()
            }
            .collect {
              case s: String => s
            }
            .toVector
        case s: String =>
          Vector(s)
      }
      .flatten
}
