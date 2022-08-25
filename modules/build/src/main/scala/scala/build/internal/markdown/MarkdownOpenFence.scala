package scala.build.internal.markdown

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
    MarkdownCodeBlock(
      info.split("\\s+").toList, // strip info by whitespaces
      bodyLines.tail.foldLeft(bodyLines.head)((body, line) => body.:++("\n" + line)),
      start,          // snippet has to begin in the new line
      tickEndLine - 1 // ending backticks have to be placed below the snippet
    )
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
