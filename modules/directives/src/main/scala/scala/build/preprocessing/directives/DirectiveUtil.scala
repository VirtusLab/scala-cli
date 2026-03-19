package scala.build.preprocessing.directives

import dependency.AnyDependency
import dependency.parser.DependencyParser

import scala.build.Ops.*
import scala.build.errors.{BuildException, CompositeBuildException, DependencyFormatError}
import scala.build.{Position, Positioned}
import scala.cli.parse.DirectiveValue

object DirectiveUtil {
  def isWrappedInDoubleQuotes(v: DirectiveValue): Boolean =
    v.isQuotedString

  def position(v: DirectiveValue, path: Either[String, os.Path]): Position.File = {
    val p          = v.pos
    val skipQuotes = v.isQuotedString
    val column     = p.column + (if skipQuotes then 1 else 0)
    val endCol     = column + v.stringValue.length
    Position.File(path, (p.line, column), (p.line, endCol))
  }

  def concatAllValues(
    scopedDirective: ScopedDirective
  ): Seq[String] =
    scopedDirective.directive.values.collect:
      case v: DirectiveValue.StringVal => v.value
      case v: DirectiveValue.BoolVal   => v.value.toString

  def positions(values: Seq[DirectiveValue], path: Either[String, os.Path]): Seq[Position] =
    values.map { v =>
      val p = v.pos
      Position.File(path, (p.line, p.column), (p.line, p.column))
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
