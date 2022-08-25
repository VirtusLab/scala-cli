package scala.build.internal.markdown

import scala.annotation.tailrec
import scala.build.internal.AmmUtil
import scala.build.internal.markdown.MarkdownCodeBlock
import scala.build.internal.Name

/** A util for extraction and wrapping of code blocks in Markdown files.
  */
object MarkdownCodeWrapper {

  /** Extracts scala code blocks from Markdown snippets, divides them into 3 categories and wraps
    * when necessary.
    *
    * @param subPath
    *   the project [[os.SubPath]] to the Markdown file
    * @param content
    *   Markdown code
    * @return
    *   a tuple of (Option(simple scala snippets code), Option(raw scala snippets code), Option(test
    *   scala snippets code))
    */
  def apply(
    subPath: os.SubPath,
    content: String
  ): (Option[String], Option[String], Option[String]) = {
    val (pkg, wrapper) = AmmUtil.pathToPackageWrapper(subPath)
    val maybePkgString =
      if pkg.isEmpty then None else Some(s"package ${AmmUtil.encodeScalaSourcePath(pkg)}")
    val allSnippets                      = MarkdownCodeBlock.findCodeBlocks(content)
    val (rawSnippets, processedSnippets) = allSnippets.partition(_.isRaw)
    val (testSnippets, mainSnippets)     = processedSnippets.partition(_.isTest)
    val wrapperName                      = s"${wrapper.raw}_md"
    (
      wrapScalaCode(mainSnippets, wrapperName, maybePkgString),
      rawScalaCode(rawSnippets),
      rawScalaCode(testSnippets)
    )
  }

  /** Scope object name for a given index
    * @param index
    *   scope index
    * @return
    *   scope name
    */
  private def scopeObjectName(index: Int): String = if index != 0 then s"Scope$index" else "Scope"

  /** Transforms [[MarkdownCodeBlock]] code blocks into code in the [[Option]] of String format
    *
    * @param snippets
    *   extracted [[MarkdownCodeBlock]] code blocks
    * @param f
    *   a function transforming a sequence of code blocks into a single String of code
    * @return
    *   an Option of the resulting code String, if any
    */
  private def code(
    snippets: Seq[MarkdownCodeBlock],
    f: Seq[MarkdownCodeBlock] => String
  ): Option[String] =
    if snippets.isEmpty then None else Some(AmmUtil.normalizeNewlines(f(snippets)))

  /** Wraps plain `scala` snippets in relevant scope objects, forming a script-like wrapper.
    *
    * @param snippets
    *   a sequence of code blocks
    * @param wrapperName
    *   name for the wrapper object
    * @param pkg
    *   package for the wrapper object
    * @return
    *   an option of the wrapped code String
    */
  def wrapScalaCode(
    snippets: Seq[MarkdownCodeBlock],
    wrapperName: String,
    pkg: Option[String]
  ): Option[String] =
    code(
      snippets,
      s => {
        val packageDirective = pkg.map(_ + "; ").getOrElse("")
        val noWarnAnnotation = """@annotation.nowarn("msg=pure expression does nothing")"""
        val firstLine =
          s"""${packageDirective}object $wrapperName { $noWarnAnnotation def main(args: Array[String]): Unit = { """
        s.indices.foldLeft(0 -> firstLine) {
          case ((nextScopeIndex, sum), index) =>
            if snippets(index).resetScope || index == 0 then
              nextScopeIndex + 1 -> (sum :++ s"${scopeObjectName(nextScopeIndex)}; ")
            else nextScopeIndex  -> sum // that class hasn't been created
        }
          ._2
          .:++("}")
          .:++(generateMainScalaLines(s, 0, 0, 0))
          .:++("}")
      }
    )

  @tailrec
  private def generateMainScalaLines(
    snippets: Seq[MarkdownCodeBlock],
    index: Int,
    scopeIndex: Int,
    line: Int,
    acc: String = ""
  ): String =
    if (index >= snippets.length) s"$acc}" // close last class
    else {
      val fence: MarkdownCodeBlock = snippets(index)
      val classOpener: String =
        if (index == 0)
          s"object ${scopeObjectName(scopeIndex)} {${System.lineSeparator()}" // first snippet needs to open a class
        else if (fence.resetScope)
          s"}; object ${scopeObjectName(scopeIndex)} {${System.lineSeparator()}" // if scope is being reset, close previous class and open a new one
        else System.lineSeparator()
      val nextScopeIndex = if index == 0 || fence.resetScope then scopeIndex + 1 else scopeIndex
      val newAcc = acc + (System.lineSeparator() * (fence.startLine - line - 1)) // padding
        .:++(classOpener)            // new class opening (if applicable)
        .:++(fence.body)             // snippet body
        .:++(System.lineSeparator()) // padding in place of closing backticks
      generateMainScalaLines(
        snippets = snippets,
        index = index + 1,
        scopeIndex = nextScopeIndex,
        line = fence.endLine + 1,
        acc = newAcc
      )
    }

  /** Glues raw Scala snippets into a single file.
    *
    * @param snippets
    *   a sequence of code blocks
    * @return
    *   an option of the resulting code String
    */
  def rawScalaCode(snippets: Seq[MarkdownCodeBlock]): Option[String] =
    code(snippets, generateRawScalaLines(_, 0, 0))

  @tailrec
  private def generateRawScalaLines(
    snippets: Seq[MarkdownCodeBlock],
    index: Int,
    line: Int,
    acc: String = ""
  ): String =
    if index >= snippets.length then acc
    else {
      val fence: MarkdownCodeBlock = snippets(index)
      val newAcc = acc + (System.lineSeparator() * (fence.startLine - line)) // padding
        .:++(fence.body)             // snippet body
        .:++(System.lineSeparator()) // padding in place of closing backticks
      generateRawScalaLines(snippets, index + 1, fence.endLine + 1, newAcc)
    }
}
