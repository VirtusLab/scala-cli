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

  /** Checks whether the directive with the sequence of values will fit into the given column limit,
    * if it does then the function returns the single directive with all the values. If the
    * directive does not fit then the function explodes it into a sequence of directives with
    * distinct values, each with a single value.
    */
  def explodeToStringsWithColLimit(colLimit: Int = 100): Seq[String] = {
    val validValues = values.filter {
      case _: EmptyValue => false
      case _             => true
    }

    val usingKeyString = s"//> using $key"

    if (validValues.isEmpty)
      Seq(usingKeyString)
    else {
      val distinctValuesStrings = validValues
        .map(v => s"\"${v.toString}\"")
        .distinct
        .sorted

      if (distinctValuesStrings.map(_.length).sum + usingKeyString.length < colLimit)
        Seq(s"$usingKeyString ${distinctValuesStrings.mkString(" ")}")
      else
        distinctValuesStrings.map(v => s"$usingKeyString $v")
    }
  }

  def stringValuesCount: Int =
    values.count {
      case _: StringValue => true
      case _              => false
    }
}
