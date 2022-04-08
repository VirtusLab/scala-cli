package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.{BooleanValue, NumericValue, StringValue, Value}

import scala.build.Positioned
import scala.build.preprocessing.ScopePath

case class ScopedValue[T <: Value[_]](
  positioned: Positioned[String],
  maybeScopePath: Option[ScopePath] = None
) {
//  override def toString = s"$positioned of type ${kind(this)}${maybeScopePath.map { scopePath =>
//      s" with scope path $scopePath"
//    }.getOrElse("")}"

  def kind(scopedValue: ScopedValue[_]) = scopedValue match {
    case _: ScopedValue[StringValue]  => UsingDirectiveValueKind.STRING
    case _: ScopedValue[NumericValue] => UsingDirectiveValueKind.NUMERIC
    case _: ScopedValue[BooleanValue] => UsingDirectiveValueKind.BOOLEAN
    case _: ScopedValue[EmptyValue] => UsingDirectiveValueKind.EMPTY
    case _                            => UsingDirectiveValueKind.UNKNOWN
  }

}
