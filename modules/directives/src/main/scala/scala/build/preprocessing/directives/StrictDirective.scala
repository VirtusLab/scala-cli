package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.{NumericValue, StringValue, Value}

case class StrictDirective(
  key: String,
  values: Seq[Value[_]]
) {
  def numericalOrStringValuesCount: Int =
    values.count {
      case _: NumericValue => true
      case _: StringValue  => true
      case _               => false
    }
}
