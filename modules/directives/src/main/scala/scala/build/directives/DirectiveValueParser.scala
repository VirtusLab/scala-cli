package scala.build.directives

import com.virtuslab.using_directives.custom.model.{
  BooleanValue,
  EmptyValue,
  NumericValue,
  StringValue,
  Value
}

import scala.build.Positioned.apply
import scala.build.errors.{
  BuildException,
  CompositeBuildException,
  MalformedDirectiveError,
  UsingDirectiveValueNumError,
  UsingDirectiveWrongValueTypeError
}
import scala.build.preprocessing.ScopePath
import scala.build.preprocessing.directives.DirectiveUtil
import scala.build.{Position, Positioned}

abstract class DirectiveValueParser[+T] {
  def parse(
    values: Seq[Value[_]],
    scopePath: ScopePath,
    path: Either[String, os.Path]
  ): Either[BuildException, T]

  final def map[U](f: T => U): DirectiveValueParser[U] =
    new DirectiveValueParser.Mapped[T, U](this, f)
}

object DirectiveValueParser {

  private final class Mapped[T, +U](underlying: DirectiveValueParser[T], f: T => U)
      extends DirectiveValueParser[U] {
    def parse(
      values: Seq[Value[_]],
      scopePath: ScopePath,
      path: Either[String, os.Path]
    ): Either[BuildException, U] =
      underlying.parse(values, scopePath, path).map(f)
  }

  abstract class DirectiveSingleValueParser[+T] extends DirectiveValueParser[T] {
    def parseValue(
      value: Value[_],
      cwd: ScopePath,
      path: Either[String, os.Path]
    ): Either[BuildException, T]

    final def parse(
      values: Seq[Value[_]],
      scopePath: ScopePath,
      path: Either[String, os.Path]
    ): Either[BuildException, T] =
      values.filter(!_.isEmpty) match {
        case Seq(value) => parseValue(value, scopePath, path)
        case _ =>
          Left(
            new UsingDirectiveValueNumError(
              path,
              "",
              "1",
              values.length
            )
          )
      }
  }

  given DirectiveValueParser[Unit] = { (values, scopePath, path) =>
    values match {
      case Seq() => Right(())
      case Seq(value, _*) =>
        val pos = value.position(path)
        Left(new MalformedDirectiveError("Expected no value in directive", Seq(pos)))
    }
  }

  extension (value: Value[_]) {

    def isEmpty: Boolean =
      value match {
        case _: EmptyValue => true
        case _             => false
      }

    def isString: Boolean =
      value match {
        case _: StringValue => true
        case _              => false
      }
    def asString: Option[String] =
      value match {
        case s: StringValue => Some(s.get())
        case _              => None
      }
    def isBoolean: Boolean =
      value match {
        case _: BooleanValue => true
        case _               => false
      }
    def asBoolean: Option[Boolean] =
      value match {
        case s: BooleanValue => Some(s.get())
        case _               => None
      }
    def asNum: Option[String] =
      value match {
        case n: NumericValue => Some(n.get())
        case _               => None
      }

    def position(path: Either[String, os.Path]): Position =
      DirectiveUtil.position(value, path, skipQuotes = isString)
  }

  given DirectiveValueParser[Boolean] = { (values, scopePath, path) =>
    values.filter(!_.isEmpty) match {
      case Seq() => Right(true)
      case Seq(v) =>
        v.asBoolean.toRight {
          new UsingDirectiveWrongValueTypeError(
            path,
            "",
            Seq("boolean"),
            ""
          )
        }
      case values0 =>
        Left(
          new MalformedDirectiveError(
            s"Unexpected values ${values0.map(_.toString).mkString(", ")}",
            values0.map(_.position(path))
          )
        )
    }
  }

  given DirectiveSingleValueParser[String] =
    (value, scopePath, path) =>
      value.asString.toRight {
        val pos = value.position(path)
        new MalformedDirectiveError(
          s"Expected a string, got '${value.getRelatedASTNode.toString}'",
          Seq(pos)
        )
      }

  final case class MaybeNumericalString(value: String)

  given DirectiveSingleValueParser[MaybeNumericalString] =
    (value, scopePath, path) =>
      value.asString.orElse(value.asNum).map(MaybeNumericalString(_)).toRight {
        val pos = value.position(path)
        new MalformedDirectiveError(
          s"Expected a string or a numerical value, got '${value.getRelatedASTNode.toString}'",
          Seq(pos)
        )
      }

  final case class WithScopePath[+T](scopePath: ScopePath, value: T)

  object WithScopePath {
    def empty[T](value: T): WithScopePath[T] =
      WithScopePath(ScopePath(Left("invalid"), os.sub), value)
  }

  given [T](using underlying: DirectiveValueParser[T]): DirectiveValueParser[WithScopePath[T]] = {
    (values, scopePath, path) =>
      underlying.parse(values, scopePath, path)
        .map(WithScopePath(scopePath, _))
  }
  given [T](using
    underlying: DirectiveSingleValueParser[T]
  ): DirectiveSingleValueParser[Positioned[T]] = {
    (value, scopePath, path) =>
      underlying.parseValue(value, scopePath, path)
        .map(Positioned(value.position(path), _))
  }
  given [T](using underlying: DirectiveValueParser[T]): DirectiveValueParser[Option[T]] =
    underlying.map(Some(_))
  given [T](using underlying: DirectiveSingleValueParser[T]): DirectiveValueParser[List[T]] = {
    (values, scopePath, path) =>
      val res = values.filter(!_.isEmpty).map(underlying.parseValue(_, scopePath, path))
      val errors = res.collect {
        case Left(e) => e
      }
      if (errors.isEmpty) Right(res.collect { case Right(v) => v }.toList)
      else Left(CompositeBuildException(errors))
  }

}
