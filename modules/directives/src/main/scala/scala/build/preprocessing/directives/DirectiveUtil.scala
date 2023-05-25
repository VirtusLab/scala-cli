package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.{BooleanValue, EmptyValue, StringValue, Value}
import com.virtuslab.using_directives.custom.utils.ast.StringLiteral
import dependency.AnyDependency
import dependency.parser.DependencyParser

import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.errors.{BuildException, CompositeBuildException, DependencyFormatError}
import scala.build.preprocessing.BuildDirectiveException
import scala.build.{Position, Positioned}
import scala.cli.commands.SpecificationLevel
import scala.cli.directivehandler.*
import scala.cli.directivehandler.DirectiveValueParser.{DirectiveSingleValueParser, position}

object DirectiveUtil {

  def position(
    v: Value[_],
    path: Either[String, os.Path]
  ): Position.File = {
    val skipQuotes: Boolean = v match {
      case stringValue: StringValue =>
        stringValue.getRelatedASTNode match {
          case literal: StringLiteral => literal.getIsWrappedDoubleQuotes()
          case _                      => false
        }
      case _ => false
    }
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
        val pos = position(v, scopedDirective.maybePath)
        Positioned(pos, v.get)
      case v: BooleanValue =>
        val pos = position(v, scopedDirective.maybePath)
        Positioned(pos, v.get.toString)
      case v: EmptyValue =>
        val pos = position(v, scopedDirective.maybePath)
        Positioned(pos, v.get)
    }

  def positions(
    values: Seq[Value[_]],
    path: Either[String, os.Path]
  ): Seq[Position.File] =
    values.map { v =>
      val line   = v.getRelatedASTNode.getPosition.getLine
      val column = v.getRelatedASTNode.getPosition.getColumn
      Position.File(path, (line, column), (line, column))
    }

  extension (deps: List[Positioned[String]]) {
    def asDependencies: Either[BuildException, Seq[Positioned[AnyDependency]]] =
      deps
        .map {
          _.map { str =>
            DependencyParser.parse(str).left.map(new DependencyFormatError(str, _))
          }.eitherSequence
        }
        .sequence
        .left.map(CompositeBuildException(_))
  }

  given [T](using
    underlying: DirectiveSingleValueParser[T]
  ): DirectiveSingleValueParser[scala.build.Positioned[T]] = {
    (value, scopePath, path) =>
      underlying.parseValue(value, scopePath, path).map { v =>
        val pos = value.position(path) match {
          case f: scala.cli.directivehandler.Position.File =>
            scala.build.Position.File(f.path, f.startPos, f.endPos)
        }
        scala.build.Positioned(pos, v)
      }
  }

  extension (pos: scala.cli.directivehandler.Position) {
    def toScalaCli: scala.build.Position = pos match {
      case f: scala.cli.directivehandler.Position.File =>
        scala.build.Position.File(f.path, f.startPos, f.endPos)
    }
  }

  extension (pos: scala.cli.directivehandler.Position.File) {
    def toScalaCli: scala.build.Position.File =
      scala.build.Position.File(pos.path, pos.startPos, pos.endPos)
  }

  extension [T](handler: DirectiveHandler[T]) {
    def scalaSpecificationLevel: SpecificationLevel =
      handler.tags
        .iterator
        .flatMap(scala.cli.commands.tags.levelFor(_).iterator)
        .take(1)
        .toList
        .headOption
        .getOrElse(SpecificationLevel.IMPLEMENTATION)
    final def isRestricted: Boolean   = scalaSpecificationLevel == SpecificationLevel.RESTRICTED
    final def isExperimental: Boolean = scalaSpecificationLevel == SpecificationLevel.EXPERIMENTAL

    def mapE[U](f: T => Either[BuildException, U]): DirectiveHandler[U] =
      new DirectiveHandler[U] {
        def name                   = handler.name
        def usage                  = handler.usage
        override def usageMd       = handler.usageMd
        def description            = handler.description
        override def descriptionMd = handler.descriptionMd
        override def examples      = handler.examples
        override def tags          = handler.tags

        def keys = handler.keys

        def handleValues(scopedDirective: ScopedDirective) =
          handler.handleValues(scopedDirective)
            .left.map(new BuildDirectiveException(_))
            .flatMap(_.mapE(f(_)))
            .left.map(DirectiveBuildException(_))
      }
  }

  extension [T](directive: ProcessedDirective[T]) {
    def mapE[U](f: T => Either[BuildException, U])
      : Either[BuildException, ProcessedDirective[U]] = {
      val maybeGlobal = directive.global.map(f) match {
        case None           => Right(None)
        case Some(Left(e))  => Left(e)
        case Some(Right(u)) => Right(Some(u))
      }
      val maybeScoped =
        directive.scoped.map(_.mapE(f)).sequence.left.map(CompositeBuildException(_))
      (maybeGlobal, maybeScoped)
        .traverseN
        .left.map(CompositeBuildException(_))
        .map {
          case (global0, scoped0) =>
            ProcessedDirective(global0, scoped0)
        }
    }
  }

  extension [T](scoped: Scoped[T]) {
    def mapE[U](f: T => Either[BuildException, U]): Either[BuildException, Scoped[U]] =
      f(scoped.value).map(u => scoped.copy(value = u))
  }
}
