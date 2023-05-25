package scala.build.preprocessing

import scala.build.EitherCps.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.markdown.MarkdownCodeBlock
import scala.build.preprocessing.BuildDirectiveException
import scala.cli.directivehandler.{DirectiveException, ExtractedDirectives, ScopePath}

object MarkdownCodeBlockProcessor {
  def process(
    codeBlocks: Seq[MarkdownCodeBlock],
    reportingPath: Either[String, os.Path],
    scopePath: ScopePath,
    maybeRecoverOnError: DirectiveException => Option[DirectiveException]
  ): Either[BuildException, PreprocessedMarkdown] = either {
    val (rawCodeBlocks, remaining)         = codeBlocks.partition(_.isRaw)
    val (testCodeBlocks, scriptCodeBlocks) = remaining.partition(_.isTest)
    def preprocessCodeBlocks(cbs: Seq[MarkdownCodeBlock])
      : Either[BuildException, PreprocessedMarkdownCodeBlocks] = either {
      val mergedDirectives: ExtractedDirectives = cbs
        .map { cb =>
          value {
            ExtractedDirectives.from(
              contentChars = cb.body.toCharArray,
              path = reportingPath,
              maybeRecoverOnError = maybeRecoverOnError
            ).left.map(new BuildDirectiveException(_))
          }
        }
        .fold(ExtractedDirectives.empty)(_ ++ _)
      PreprocessedMarkdownCodeBlocks(
        cbs,
        mergedDirectives
      )
    }
    PreprocessedMarkdown(
      value(preprocessCodeBlocks(scriptCodeBlocks)),
      value(preprocessCodeBlocks(rawCodeBlocks)),
      value(preprocessCodeBlocks(testCodeBlocks))
    )
  }
}
