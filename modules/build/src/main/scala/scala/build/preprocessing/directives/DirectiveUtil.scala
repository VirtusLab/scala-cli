package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.{NumericValue, StringValue, Value}

import scala.build.Position
import scala.build.preprocessing.ScopePath

object DirectiveUtil {
  def stringValues(
    values: Seq[Value[_]],
    path: Either[String, os.Path],
    cwd: ScopePath
  ): Seq[(String, Position, Option[ScopePath])] =
    values
      .collect {
        case v: StringValue =>
          val line   = v.getRelatedASTNode.getPosition.getLine
          val column = v.getRelatedASTNode.getPosition.getColumn
          Seq((
            v.get,
            Position.File(path, (line, column), (line, column)),
            Option(v.getScope).map((p: String) => cwd / os.RelPath(p))
          ))
      }
      .flatten

  def numericValues(
    values: Seq[Value[_]],
    path: Either[String, os.Path],
    cwd: ScopePath
  ): Seq[(String, Position, Option[ScopePath])] =
    values
      .collect {
        case v: NumericValue =>
          val line   = v.getRelatedASTNode.getPosition.getLine
          val column = v.getRelatedASTNode.getPosition.getColumn
          Seq((
            v.get,
            Position.File(path, (line, column), (line, column)),
            Option(v.getScope).map((p: String) => cwd / os.RelPath(p))
          ))
      }
      .flatten
}
