package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.{
  BooleanValue,
  EmptyValue,
  NumericValue,
  StringValue,
  Value
}

import scala.build.Positioned
import scala.build.preprocessing.ScopePath

case class ScopedValue[T <: Value[_]](
  positioned: Positioned[String],
  maybeScopePath: Option[ScopePath] = None
) {

  def kind(scopedValue: ScopedValue[_]) = scopedValue match {
    case _: ScopedValue[StringValue]  => UsingDirectiveValueKind.STRING
    case _: ScopedValue[NumericValue] => UsingDirectiveValueKind.NUMERIC
    case _: ScopedValue[BooleanValue] => UsingDirectiveValueKind.BOOLEAN
    case _: ScopedValue[EmptyValue]   => UsingDirectiveValueKind.EMPTY
    case _                            => UsingDirectiveValueKind.UNKNOWN
  }

}
