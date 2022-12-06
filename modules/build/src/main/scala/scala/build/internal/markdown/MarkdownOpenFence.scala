package scala.build.internal.markdown

import scala.build.Position
import scala.build.errors.MarkdownUnclosedBackticksError
import scala.build.preprocessing.SheBang

/** Representation for an open code block in Markdown. (open meaning the closing backticks haven't
  * yet been parsed or they aren't at all present)
  *
  * @param info
  *   a list of tags tied to a given code block
  * @param tickStartLine
  *   index of the starting line on which the opening backticks were defined
  * @param backticks
  *   the backticks string opening the fence
  * @param indent
  *   number of spaces of indentation for the fence
  */
case class MarkdownOpenFence(
  info: String,
  tickStartLine: Int, // fence start INCLUDING backticks
  backticks: String,
  indent: Int
) {

  /** Closes started code-fence
    *
    * @param tickEndLine
    *   number of the line where closing backticks are
    * @param lines
    *   input file sliced into lines
    * @return
    *   [[MarkdownCodeBlock]] representing whole closed code-fence
    */
  def closeFence(
    tickEndLine: Int,
    lines: Array[String]
  ): MarkdownCodeBlock = {
    val start: Int               = tickStartLine + 1
    val bodyLines: Array[String] = lines.slice(start, tickEndLine)
    val body                     = bodyLines.mkString("\n")
    val (bodyWithNoSheBang, _)   = SheBang.ignoreSheBangLines(body)
    MarkdownCodeBlock(
      info.split("\\s+").toList, // strip info by whitespaces
      bodyWithNoSheBang,
      start,          // snippet has to begin in the new line
      tickEndLine - 1 // ending backticks have to be placed below the snippet
    )
  }

  /** Converts the [[MarkdownOpenFence]] into a [[MarkdownUnclosedBackticksError]]
    *
    * @param mdPath
    *   path to the Markdown file
    * @return
    *   a [[MarkdownUnclosedBackticksError]]
    */
  def toUnclosedBackticksError(mdPath: os.Path): MarkdownUnclosedBackticksError = {
    val startCoordinates = tickStartLine -> indent
    val endCoordinates =
      tickStartLine -> (indent + backticks.length)
    val position = Position.File(Right(mdPath), startCoordinates, endCoordinates)
    MarkdownUnclosedBackticksError(backticks, Seq(position))
  }
}

object MarkdownOpenFence {
  def maybeFence(line: String, index: Int): Option[MarkdownOpenFence] = {
    val start: Int = line.indexOf("```")
    if (start >= 0) {
      val fence             = line.substring(start)
      val backticks: String = fence.takeWhile(_ == '`')
      val info: String      = fence.substring(backticks.length)
      Some(MarkdownOpenFence(info, index, backticks, start))
    }
    else None
  }
}
