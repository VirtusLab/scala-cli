package scala.build.preprocessing

import scala.build.internal.markdown.MarkdownCodeBlock

case class PreprocessedMarkdownCodeBlocks(
  codeBlocks: Seq[MarkdownCodeBlock],
  extractedDirectives: ExtractedDirectives = ExtractedDirectives.empty
)

object PreprocessedMarkdownCodeBlocks {
  def empty: PreprocessedMarkdownCodeBlocks =
    PreprocessedMarkdownCodeBlocks(Seq.empty, ExtractedDirectives.empty)
}

case class PreprocessedMarkdown(
  scriptCodeBlocks: PreprocessedMarkdownCodeBlocks = PreprocessedMarkdownCodeBlocks.empty,
  rawCodeBlocks: PreprocessedMarkdownCodeBlocks = PreprocessedMarkdownCodeBlocks.empty,
  testCodeBlocks: PreprocessedMarkdownCodeBlocks = PreprocessedMarkdownCodeBlocks.empty
)

object PreprocessedMarkdown {
  def empty: PreprocessedMarkdown = PreprocessedMarkdown()
}
