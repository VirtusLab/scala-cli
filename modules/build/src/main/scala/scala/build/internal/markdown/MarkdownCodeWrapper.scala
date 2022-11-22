package scala.build.internal.markdown

import scala.annotation.tailrec
import scala.build.errors.BuildException
import scala.build.internal.markdown.MarkdownCodeBlock
import scala.build.internal.{AmmUtil, Name}
import scala.build.preprocessing.{
  ExtractedDirectives,
  PreprocessedMarkdown,
  PreprocessedMarkdownCodeBlocks
}

/** A util for extraction and wrapping of code blocks in Markdown files.
  */
object MarkdownCodeWrapper {

  case class WrappedMarkdownCode(
    code: String,
    directives: ExtractedDirectives = ExtractedDirectives.empty
  )

  /** Extracts scala code blocks from Markdown snippets, divides them into 3 categories and wraps
    * when necessary.
    *
    * @param subPath
    *   the project [[os.SubPath]] to the Markdown file
    * @param markdown
    *   preprocessed Markdown code blocks
    * @return
    *   a tuple of (Option(simple scala code blocks), Option(raw scala snippets code blocks),
    *   Option(test scala snippets code blocks))
    */
  def apply(
    subPath: os.SubPath,
    markdown: PreprocessedMarkdown
  ): (Option[WrappedMarkdownCode], Option[WrappedMarkdownCode], Option[WrappedMarkdownCode]) = {
    val (pkg, wrapper) = AmmUtil.pathToPackageWrapper(subPath)
    val maybePkgString =
      if pkg.isEmpty then None else Some(s"package ${AmmUtil.encodeScalaSourcePath(pkg)}")
    val wrapperName = s"${wrapper.raw}_md"
    (
      wrapScalaCode(markdown.scriptCodeBlocks, wrapperName, maybePkgString),
      rawScalaCode(markdown.rawCodeBlocks),
      rawScalaCode(markdown.testCodeBlocks)
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
    preprocessed: PreprocessedMarkdownCodeBlocks,
    wrapperName: String,
    pkg: Option[String]
  ): Option[WrappedMarkdownCode] =
    code(
      preprocessed.codeBlocks,
      s => {
        val packageDirective = pkg.map(_ + "; ").getOrElse("")
        val noWarnAnnotation = """@annotation.nowarn("msg=pure expression does nothing")"""
        val firstLine =
          s"""${packageDirective}object $wrapperName { $noWarnAnnotation def main(args: Array[String]): Unit = { """
        s.indices.foldLeft(0 -> firstLine) {
          case ((nextScopeIndex, sum), index) =>
            if preprocessed.codeBlocks(index).resetScope || index == 0 then
              nextScopeIndex + 1 -> (sum :++ s"${scopeObjectName(nextScopeIndex)}; ")
            else nextScopeIndex  -> sum // that class hasn't been created
        }
          ._2
          .:++("}")
          .:++(generateMainScalaLines(s, 0, 0, 0))
          .:++("}")
      }
    ).map(c => WrappedMarkdownCode(c, preprocessed.extractedDirectives))

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
  def rawScalaCode(preprocessed: PreprocessedMarkdownCodeBlocks): Option[WrappedMarkdownCode] =
    code(preprocessed.codeBlocks, generateRawScalaLines(_, 0, 0))
      .map(c => WrappedMarkdownCode(c, preprocessed.extractedDirectives))

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
