package scala.build.errors

import scala.build.preprocessing.directives.UsingDirectiveValueKind.UsingDirectiveValueKind
import scala.build.preprocessing.directives.{
  GroupedScopedValuesContainer,
  UsingDirectiveValueNumberBounds
}

sealed abstract class UsingDirectiveExpectationError(
  message: String
) extends BuildException(message)

final class UsingDirectiveWrongValueTypeError(
  maybePath: Either[String, os.Path],
  key: String,
  expectedTypes: Set[UsingDirectiveValueKind],
  providedPositionedTypesContainer: GroupedScopedValuesContainer,
  hint: String = ""
) extends UsingDirectiveExpectationError(
      s"""${expectedTypes.mkString(
          ", or "
        )} expected for the $key using directive key${maybePath.map(path => s" at $path").getOrElse(
          ""
        )}; but $providedPositionedTypesContainer  provided.
         |$hint""".stripMargin
    )

final class UsingDirectiveValueNumError(
  maybePath: Either[String, os.Path],
  key: String,
  usingDirectiveValueNumberBounds: UsingDirectiveValueNumberBounds,
  providedValueNum: Int
) extends UsingDirectiveExpectationError(
      s"expected $usingDirectiveValueNumberBounds for the $key using directive key${maybePath.map(path => s" at $path").getOrElse(
          ""
        )}; but got $providedValueNum values, instead."
    ) {}
