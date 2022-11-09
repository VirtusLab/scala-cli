package scala.build.internal.markdown

import scala.annotation.tailrec
import scala.build.Position
import scala.build.errors.{BuildException, MarkdownUnclosedBackticksError}
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
    * @param subPath
    *   the project [[os.SubPath]] to the Markdown file
    * @param md
    *   Markdown file in a `String` format
    * @param maybeRecoverOnError
    *   function potentially recovering on errors
    * @return
    *   list of all found snippets
    */
  def findCodeBlocks(
    subPath: os.SubPath,
    md: String,
    maybeRecoverOnError: BuildException => Option[BuildException]
  ): Either[BuildException, Seq[MarkdownCodeBlock]] = {
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
    ): Either[BuildException, Seq[MarkdownCodeBlock]] = lines -> maybeOpenFence match {
      case (Seq(currentLine, tail*), mof) =>
        val (newClosedFences, newOpenFence) = mof match {
          case None => closedFences -> MarkdownOpenFence.maybeFence(currentLine, currentIndex)
          case Some(openFence) =>
            val backticksStart = currentLine.indexOf(openFence.backticks)
            if backticksStart == openFence.indent &&
              currentLine.forall(c => c == '`' || c.isWhitespace)
            then (closedFences :+ openFence.closeFence(currentIndex, allLines.toArray)) -> None
            else closedFences -> Some(openFence)
        }
        findCodeBlocksRec(tail, newClosedFences, newOpenFence, currentIndex + 1)
      case (Nil, Some(openFence)) =>
        maybeRecoverOnError(openFence.toUnclosedBackticksError(os.pwd / subPath))
          .map(e => Left(e))
          .getOrElse(Right(closedFences))
      case _ => Right(closedFences)
    }
    findCodeBlocksRec(allLines.toSeq).map(_.filter(!_.shouldIgnore))
  }
}
