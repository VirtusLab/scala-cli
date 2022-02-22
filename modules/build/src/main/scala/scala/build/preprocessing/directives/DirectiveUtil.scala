package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.{BooleanValue, NumericValue, StringValue, Value}

import scala.build.preprocessing.ScopePath
import scala.build.{Position, Positioned}

object DirectiveUtil {
  def stringValues(
    values: Seq[Value[_]],
    path: Either[String, os.Path],
    cwd: ScopePath
  ): Seq[(Positioned[String], Option[ScopePath])] =
    values.collect {
      case v: StringValue =>
        println("v.get as StringValue: "+ v.get())
        val line   = v.getRelatedASTNode.getPosition.getLine
        val column = v.getRelatedASTNode.getPosition.getColumn + 1 // Using directives are 0 based
        val pos    = Position.File(path, (line, column), (line, column))
        (
          Positioned(pos, v.get),
          Option(v.getScope).map((p: String) => cwd / os.RelPath(p))
        )
      case v: NumericValue =>
        println("v.get as NumericValue: "+ v.get())
        val line   = v.getRelatedASTNode.getPosition.getLine
        val column = v.getRelatedASTNode.getPosition.getColumn
        val pos    = Position.File(path, (line, column), (line, column))
        (
          Positioned(pos, v.get),
          Option(v.getScope).map((p: String) => cwd / os.RelPath(p))
        )
//      case v: BooleanValue =>
//        val line   = v.getRelatedASTNode.getPosition.getLine
//        val column = v.getRelatedASTNode.getPosition.getColumn
//        val pos    = Position.File(path, (line, column), (line, column))
//        (
//          Positioned(pos, v.get),
//          Option(v.getScope).map((p: String) => cwd / os.RelPath(p))
//        )
    }

  def numericValues(
    values: Seq[Value[_]],
    path: Either[String, os.Path],
    cwd: ScopePath
  ): Seq[(Positioned[String], Option[ScopePath])] =
    values.collect {
      case v: NumericValue =>
        val line   = v.getRelatedASTNode.getPosition.getLine
        val column = v.getRelatedASTNode.getPosition.getColumn
        val pos    = Position.File(path, (line, column), (line, column))
        (
          Positioned(pos, v.get),
          Option(v.getScope).map((p: String) => cwd / os.RelPath(p))
        )
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
