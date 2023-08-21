package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.{EmptyValue, StringValue, Value}

case class StrictDirective(
  key: String,
  values: Seq[Value[_]]
) {
  override def toString: String = {
    val validValues = values.filter {
      case _: EmptyValue => false
      case _             => true
    }
    val suffix = if validValues.isEmpty then "" else s" \"${validValues.mkString("\",  \"")}\""
    s"//> using $key$suffix"
  }
  def stringValuesCount: Int =
    values.count {
      case _: StringValue => true
      case _              => false
    }
}
