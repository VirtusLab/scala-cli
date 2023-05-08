package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.{BooleanValue, EmptyValue, StringValue, Value}

import scala.build.preprocessing.ScopePath
import scala.build.{Position, Positioned}

object DirectiveUtil {

  def position(
    v: Value[_],
    path: Either[String, os.Path],
    skipQuotes: Boolean = false
  ): Position.File = {
    val line       = v.getRelatedASTNode.getPosition.getLine
    val column     = v.getRelatedASTNode.getPosition.getColumn + (if (skipQuotes) 1 else 0)
    val endLinePos = column + v.toString.length
    Position.File(path, (line, column), (line, endLinePos))
  }

  def scope(v: Value[_], cwd: ScopePath): Option[ScopePath] =
    Option(v.getScope).map((p: String) => cwd / os.RelPath(p))

  def concatAllValues(
    scopedDirective: ScopedDirective
  ): Seq[Positioned[String]] =
    scopedDirective.directive.values.map {
      case v: StringValue =>
        val pos = position(v, scopedDirective.maybePath, skipQuotes = true)
        Positioned(pos, v.get)
      case v: BooleanValue =>
        val pos = position(v, scopedDirective.maybePath, skipQuotes = false)
        Positioned(pos, v.get.toString)
      case v: EmptyValue =>
        val pos = position(v, scopedDirective.maybePath, skipQuotes = false)
        Positioned(pos, v.get)
    }

  def positions(
    values: Seq[Value[_]],
    path: Either[String, os.Path]
  ): Seq[Position] =
    values.map { v =>
      val line   = v.getRelatedASTNode.getPosition.getLine
      val column = v.getRelatedASTNode.getPosition.getColumn
      Position.File(path, (line, column), (line, column))
    }

}
