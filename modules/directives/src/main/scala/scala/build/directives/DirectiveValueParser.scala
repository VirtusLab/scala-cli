package scala.build.directives

import com.virtuslab.using_directives.custom.model.{BooleanValue, EmptyValue, StringValue, Value}

import scala.build.Positioned.apply
import scala.build.errors.*
import scala.build.preprocessing.ScopePath
import scala.build.preprocessing.directives.DirectiveUtil
import scala.build.{Named, Position, Positioned}
import scala.util.NotGiven

abstract class DirectiveValueParser[+T] {
  def parse(
    key: String,
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
      key: String,
      values: Seq[Value[_]],
      scopePath: ScopePath,
      path: Either[String, os.Path]
    ): Either[BuildException, U] =
      underlying.parse(key, values, scopePath, path).map(f)
  }

  abstract class DirectiveSingleValueParser[+T] extends DirectiveValueParser[T] {
    def parseValue(
      key: String,
      value: Value[_],
      cwd: ScopePath,
      path: Either[String, os.Path]
    ): Either[BuildException, T]

    final def parse(
      key: String,
      values: Seq[Value[_]],
      scopePath: ScopePath,
      path: Either[String, os.Path]
    ): Either[BuildException, T] =
      values match {
        case Seq(value) if !value.isEmpty => parseValue(key, value, scopePath, path)
        case Seq(value) if value.isEmpty && (key == "toolkit" || key == "test.toolkit") =>
          // FIXME: handle similar parsing errors in the directive declaration instead of hacks like this one
          Left(ToolkitDirectiveMissingVersionError(maybePath = path, key = key))
        case resultValues @ _ =>
          Left(
            new UsingDirectiveValueNumError(
              maybePath = path,
              key = key,
              expectedValueNum = 1,
              providedValueNum = resultValues.count(!_.isEmpty)
            )
          )
      }
  }

  given DirectiveValueParser[Unit] = { (key, values, scopePath, path) =>
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

    def position(path: Either[String, os.Path]): Position =
      DirectiveUtil.position(value, path)
  }

  given DirectiveValueParser[Boolean] = { (key, values, scopePath, path) =>
    values.filter(!_.isEmpty) match {
      case Seq() => Right(true)
      case Seq(v) =>
        v.asBoolean.toRight {
          new UsingDirectiveWrongValueTypeError(
            maybePath = path,
            key = key,
            expectedTypes = Seq("boolean"),
            hint = ""
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
    (key, value, scopePath, path) =>
      value.asString.toRight {
        val pos = value.position(path)
        new MalformedDirectiveError(
          message =
            s"""Encountered an error for the $key using directive.
               |Expected a string, got '${value.getRelatedASTNode.toString}'""".stripMargin,
          positions = Seq(pos)
        )
      }.map(DirectiveSpecialSyntax.handlingSpecialPathSyntax(_, path))

  final case class MaybeNumericalString(value: String)

  given DirectiveSingleValueParser[MaybeNumericalString] =
    (key, value, scopePath, path) =>
      value.asString.map(MaybeNumericalString(_)).toRight {
        val pos = value.position(path)
        new MalformedDirectiveError(
          s"""Encountered an error for the $key using directive.
             |Expected a string value, got '${value.getRelatedASTNode.toString}'""".stripMargin,
          Seq(pos)
        )
      }

  final case class WithScopePath[+T](scopePath: ScopePath, value: T)

  object WithScopePath {
    def empty[T](value: T): WithScopePath[T] =
      WithScopePath(ScopePath(Left("invalid"), os.sub), value)
  }

  given [T](using underlying: DirectiveValueParser[T]): DirectiveValueParser[WithScopePath[T]] = {
    (key, values, scopePath, path) =>
      underlying.parse(key, values, scopePath, path)
        .map(WithScopePath(scopePath, _))
  }
  given [T](using
    underlying: DirectiveSingleValueParser[T]
  ): DirectiveSingleValueParser[Positioned[T]] = {
    (key, value, scopePath, path) =>
      underlying.parseValue(key, value, scopePath, path)
        .map(Positioned(value.position(path), _))
  }
  given [T](using underlying: DirectiveValueParser[T]): DirectiveValueParser[Option[T]] =
    underlying.map(Some(_))
  given [T](using underlying: DirectiveSingleValueParser[T]): DirectiveValueParser[List[T]] = {
    (key, values, scopePath, path) =>
      val res = values.filter(!_.isEmpty).map(underlying.parseValue(key, _, scopePath, path))
      val errors = res.collect {
        case Left(e) => e
      }
      if (errors.isEmpty) Right(res.collect { case Right(v) => v }.toList)
      else Left(CompositeBuildException(errors))
  }

  given [T](using
    underlying: DirectiveValueParser[T],
    // TODO: nested named directives are currently not supported
    notNested: NotGiven[T <:< Named[_]]
  ): DirectiveValueParser[Named[T]] = {
    (key, values, scopePath, path) =>
      for {
        named <- Right(Named.fromKey(key))
        name  <- named.name.toRight(UnnamedKeyError(key))
        res   <- underlying.parse(named.value, values.filter(!_.isEmpty), scopePath, path)
      } yield Named(name, res)
  }
}
