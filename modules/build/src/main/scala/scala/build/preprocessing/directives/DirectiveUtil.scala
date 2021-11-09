package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.{StringValue, Value}

import scala.build.Position

object DirectiveUtil {
  def stringValues(
    values: Seq[Value[_]],
    path: Either[String, os.Path]
  ): Seq[(String, Position)] =
    values
      .collect {
        case v: StringValue =>
          val line = v.getRelatedASTNode.getPosition.getLine
          val column = v.getRelatedASTNode.getPosition.getColumn
          Seq((v.get, Position.File(path, (line, column), (line, column))))
      }
      .flatten
}
