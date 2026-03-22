package scala.build.preprocessing.directives

import scala.build.Position
import scala.cli.parse.DirectiveValue

/** Represents a directive with a key and a sequence of values.
  *
  * @param key
  *   the key of the directive
  * @param values
  *   the sequence of values of the directive
  * @param startColumn
  *   the column where the key of the directive starts
  * @param startLine
  *   the line where the key of the directive starts
  */
case class StrictDirective(
  key: String,
  values: Seq[DirectiveValue],
  startColumn: Int = 0,
  startLine: Int = 0
) {

  /** Same style as the legacy `using_directives` values' `toString` (unquoted string content), so
    * user-facing messages (e.g. experimental-feature warnings) match integration-test expectations.
    */
  override def toString: String = {
    val suffix =
      if validValues.isEmpty then "" else s" ${validValues.map(_.stringValue).mkString("  ")}"
    s"//> using $key$suffix"
  }

  private def validValues: Seq[DirectiveValue] = values.filter {
    case _: DirectiveValue.EmptyVal => false
    case _                          => true
  }

  /** Checks whether the directive with the sequence of values will fit into the given column limit,
    * if it does then the function returns the single directive with all the values. If the
    * directive does not fit then the function explodes it into a sequence of directives with
    * distinct values, each with a single value.
    */
  def explodeToStringsWithColLimit(colLimit: Int = 100): Seq[String] = {
    val validVals      = validValues
    val usingKeyString = s"//> using $key"

    if (validVals.isEmpty)
      Seq(usingKeyString)
    else {
      val distinctValuesStrings = validVals
        .map { v =>
          val s = v.stringValue
          if s.exists(_.isWhitespace) then s"\"$s\"" else s
        }
        .distinct
        .sorted

      if (distinctValuesStrings.map(_.length).sum + usingKeyString.length < colLimit)
        Seq(s"$usingKeyString ${distinctValuesStrings.mkString(" ")}")
      else
        distinctValuesStrings.map(v => s"$usingKeyString $v")
    }
  }

  def stringValuesCount: Int = validValues.length

  def toStringValues: Seq[String] = validValues.map(_.stringValue)

  def position(path: Either[String, os.Path]): Position.File =
    values.lastOption
      .map { v =>
        val p = v.pos
        v match
          case _: DirectiveValue.EmptyVal => (p.line, p.column)
          case bv: DirectiveValue.BoolVal => (p.line, p.column + bv.value.toString.length)
          case sv: DirectiveValue.StringVal if sv.isQuoted =>
            (p.line, p.column + sv.value.length + 2)
          case sv: DirectiveValue.StringVal => (p.line, p.column + sv.value.length)
      }.map { (line, endColumn) =>
        Position.File(
          path,
          (line, startColumn),
          (line, endColumn)
        )
      }.getOrElse(Position.File(path, (0, 0), (0, 0)))
}
