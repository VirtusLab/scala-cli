package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.{BooleanValue, StringValue, Value}
import com.virtuslab.using_directives.custom.utils.ast.StringLiteral
import dependency.AnyDependency
import dependency.parser.DependencyParser

import scala.build.Ops.*
import scala.build.errors.{BuildException, CompositeBuildException, DependencyFormatError}
import scala.build.preprocessing.ScopePath
import scala.build.{Position, Positioned}

object DirectiveUtil {

  def isWrappedInDoubleQuotes(v: Value[_]): Boolean =
    v match {
      case stringValue: StringValue =>
        stringValue.getRelatedASTNode match {
          case literal: StringLiteral => literal.getIsWrappedDoubleQuotes()
          case _                      => false
        }
      case _ => false
    }
  def position(
    v: Value[_],
    path: Either[String, os.Path]
  ): Position.File = {
    val skipQuotes: Boolean = isWrappedInDoubleQuotes(v)
    val line                = v.getRelatedASTNode.getPosition.getLine
    val column              = v.getRelatedASTNode.getPosition.getColumn + (if (skipQuotes) 1 else 0)
    val endLinePos          = column + v.toString.length
    Position.File(path, (line, column), (line, endLinePos))
  }

  def scope(v: Value[_], cwd: ScopePath): Option[ScopePath] =
    Option(v.getScope).map((p: String) => cwd / os.RelPath(p))

  def concatAllValues(
    scopedDirective: ScopedDirective
  ): Seq[String] =
    scopedDirective.directive.values.collect:
      case v: StringValue  => v.get
      case v: BooleanValue => v.get.toString

  def positions(
    values: Seq[Value[_]],
    path: Either[String, os.Path]
  ): Seq[Position] =
    values.map { v =>
      val line   = v.getRelatedASTNode.getPosition.getLine
      val column = v.getRelatedASTNode.getPosition.getColumn
      Position.File(path, (line, column), (line, column))
    }

  extension (deps: List[Positioned[String]]) {
    def asDependencies: Either[BuildException, Seq[Positioned[AnyDependency]]] =
      deps
        .map { positionedDep =>
          positionedDep.map { str =>
            DependencyParser.parse(str).left.map { error =>
              new DependencyFormatError(
                str,
                error,
                positions = positionedDep.positions
              )
            }
          }.eitherSequence
        }
        .sequence
        .left.map(CompositeBuildException(_))
  }
}
