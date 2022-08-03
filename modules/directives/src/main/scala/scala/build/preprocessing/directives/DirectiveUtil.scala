package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.{
  BooleanValue,
  EmptyValue,
  NumericValue,
  StringValue,
  Value
}

import scala.build.preprocessing.ScopePath
import scala.build.preprocessing.directives.UsingDirectiveValueKind.UsingDirectiveValueKind
import scala.build.{Position, Positioned}

case class GroupedScopedValuesContainer(
  scopedStringValues: Seq[ScopedValue[StringValue]] = Seq.empty,
  scopedNumericValues: Seq[ScopedValue[NumericValue]] = Seq.empty,
  scopedBooleanValues: Seq[ScopedValue[BooleanValue]] = Seq.empty,
  maybeScopedEmptyValue: Option[ScopedValue[EmptyValue]] = None
) {

  def isEmpty =
    scopedStringValues.isEmpty && scopedNumericValues.isEmpty && scopedBooleanValues.isEmpty && maybeScopedEmptyValue.isEmpty

  def size = scopedStringValues.length + scopedBooleanValues.length + scopedNumericValues.length

  override def toString = {
    if (scopedStringValues.nonEmpty) " " + scopedStringValues.map(_.positioned.value).mkString(", ")
    else ""
  } + {
    if (scopedNumericValues.nonEmpty)
      " " + scopedNumericValues.map(_.positioned.value).mkString(", ")
    else ""
  } + {
    if (scopedBooleanValues.nonEmpty)
      " " + scopedBooleanValues.map(_.positioned.value).mkString(", ")
    else ""
  } + {
    if (maybeScopedEmptyValue.nonEmpty)
      " " + maybeScopedEmptyValue.map(_.positioned.value).mkString
    else ""
  }
}

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

  def kind(v: Value[_]): UsingDirectiveValueKind = v match {
    case _: StringValue  => UsingDirectiveValueKind.STRING
    case _: NumericValue => UsingDirectiveValueKind.NUMERIC
    case _: BooleanValue => UsingDirectiveValueKind.BOOLEAN
    case _: EmptyValue   => UsingDirectiveValueKind.EMPTY
    case _               => UsingDirectiveValueKind.UNKNOWN
  }

  /** @return
    *   (scopedValuesSeq, nonScopedValuesSeq)
    */
  def partitionBasedOnHavingScope(
    groupedPositionedValuesContainer: GroupedScopedValuesContainer
  ): (Seq[(ScopedValue[_], ScopePath)], Seq[ScopedValue[_]]) = {

    val values = groupedPositionedValuesContainer.scopedStringValues ++
      groupedPositionedValuesContainer.scopedNumericValues ++
      groupedPositionedValuesContainer.scopedBooleanValues ++
      groupedPositionedValuesContainer.maybeScopedEmptyValue.toSeq

    val scoped = values.flatMap { value =>
      value.maybeScopePath.toSeq.map((value, _))
    }
    val nonScoped = values.filter(_.maybeScopePath.isEmpty)

    (scoped, nonScoped)
  }

  def concatAllValues(groupedPositionedValuesContainer: GroupedScopedValuesContainer)
    : Seq[ScopedValue[_]] =
    groupedPositionedValuesContainer.scopedStringValues ++
      groupedPositionedValuesContainer.scopedNumericValues ++
      groupedPositionedValuesContainer.scopedBooleanValues ++
      groupedPositionedValuesContainer.maybeScopedEmptyValue.to(Seq)

  def getGroupedValues(
    scopedDirective: ScopedDirective
  ): GroupedScopedValuesContainer = {
    val values = scopedDirective.directive.values
    var result = GroupedScopedValuesContainer()

    values.foreach {
      case v: StringValue =>
        val pos = position(v, scopedDirective.maybePath, skipQuotes = true)
        result = result.copy(scopedStringValues =
          result.scopedStringValues :+ ScopedValue[StringValue](
            Positioned(pos, v.get),
            scope(v, scopedDirective.cwd)
          )
        )
      case v: NumericValue =>
        val pos = position(v, scopedDirective.maybePath, skipQuotes = false)
        result = result.copy(scopedNumericValues =
          result.scopedNumericValues :+ ScopedValue[NumericValue](
            Positioned(pos, v.get),
            scope(v, scopedDirective.cwd)
          )
        )
      case v: BooleanValue =>
        val pos = position(v, scopedDirective.maybePath, skipQuotes = false)
        result = result.copy(scopedBooleanValues =
          result.scopedBooleanValues :+ ScopedValue[BooleanValue](
            Positioned(pos, v.get.toString),
            scope(v, scopedDirective.cwd)
          )
        )
      case v: EmptyValue =>
        val pos = position(v, scopedDirective.maybePath, skipQuotes = false)
        result = result.copy(maybeScopedEmptyValue =
          Some(ScopedValue[EmptyValue](
            Positioned(pos, v.get),
            scope(v, scopedDirective.cwd)
          ))
        )
    }
    result
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
