package scala.build.internal.markdown

import scala.annotation.tailrec
import scala.collection.mutable
import scala.jdk.CollectionConverters.*

/** Representation for a (closed) code block contained in Markdown
  *
  * @param info
  *   a list of tags tied to a given code block
  * @param body
  *   the code block content
  * @param startLine
  *   starting line on which the code block was defined (excluding backticks)
  * @param endLine
  *   end line on which the code block was closed (excluding backticks)
  */
case class MarkdownCodeBlock(
  info: Seq[String],
  body: String,
  startLine: Int,
  endLine: Int
) {

  /** @return
    *   `true` if this snippet should be ignored, `false` otherwise
    */
  def shouldIgnore: Boolean = info.head != "scala" || info.contains("ignore")

  /** @return
    *   `true` if this snippet should have its scope reset, `false` otherwise
    */
  def resetScope: Boolean = info.contains("reset")

  /** @return
    *   `true` if this snippet is a test snippet, `false` otherwise
    */
  def isTest: Boolean = info.contains("test")

  /** @return
    *   `true` if this snippet is a raw snippet, `false` otherwise
    */
  def isRaw: Boolean = info.contains("raw")
}

object MarkdownCodeBlock {

  /** Finds all code snippets in given input
    *
    * @param md
    *   Markdown file in a `String` format
    * @return
    *   list of all found snippets
    */
  def findCodeBlocks(md: String): Seq[MarkdownCodeBlock] = {
    val allLines = md
      .lines()
      .toList
      .asScala
    @tailrec
    def findCodeBlocksRec(
      lines: Seq[String],
      closedFences: Seq[MarkdownCodeBlock] = Seq.empty,
      maybeOpenFence: Option[MarkdownOpenFence] = None,
      currentIndex: Int = 0
    ): Seq[MarkdownCodeBlock] = if lines.isEmpty then closedFences
    else {
      val currentLine = lines.head
      val (newClosedFences, newOpenFence) = maybeOpenFence match {
        case None => closedFences -> MarkdownOpenFence.maybeFence(currentLine, currentIndex)
        case mof @ Some(openFence) =>
          val backticksStart = currentLine.indexOf(openFence.backticks)
          if backticksStart == openFence.indent &&
            currentLine.forall(c => c == '`' || c.isWhitespace)
          then (closedFences :+ openFence.closeFence(currentIndex, allLines.toArray)) -> None
          else closedFences                                                           -> mof
      }
      findCodeBlocksRec(lines.tail, newClosedFences, newOpenFence, currentIndex + 1)
    }
    findCodeBlocksRec(allLines.toSeq).filter(!_.shouldIgnore)
  }
}
