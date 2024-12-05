package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.{EmptyValue, Value}

import scala.build.Position

/** Represents a directive with a key and a sequence of values.
  *
  * @param key
  *   the key of the directive
  * @param values
  *   the sequence of values of the directive
  * @param startColumn
  *   the column where the key of the directive starts
  */

case class StrictDirective(
  key: String,
  values: Seq[Value[?]],
  startColumn: Int = 0
) {
  override def toString: String = {
    val suffix = if validValues.isEmpty then "" else s" ${validValues.mkString("  ")}"
    s"//> using $key$suffix"
  }

  private def validValues = values.filter {
    case _: EmptyValue => false
    case _             => true
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

  def stringValuesCount: Int = validValues.length

  def toStringValues: Seq[String] = validValues.map(_.toString)

  def position(path: Either[String, os.Path]): Position.File =
    values.lastOption
      .map { v =>
        val position = DirectiveUtil.position(v, path)
        v match
          case _: EmptyValue => position.startPos
          case v if DirectiveUtil.isWrappedInDoubleQuotes(v) =>
            position.endPos._1 -> (position.endPos._2 + 1)
          case _ => position.endPos
      }.map { (line, endColumn) =>
        Position.File(
          path,
          (line, startColumn),
          (line, endColumn)
        )
      }.getOrElse(Position.File(path, (0, 0), (0, 0)))

}
