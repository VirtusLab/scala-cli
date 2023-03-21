package scala.build.preprocessing

import com.virtuslab.using_directives.custom.model.UsingDirectiveKind

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.internal.markdown.MarkdownCodeBlock

object MarkdownCodeBlockProcessor {
  def process(
    codeBlocks: Seq[MarkdownCodeBlock],
    reportingPath: Either[String, os.Path],
    scopePath: ScopePath,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException]
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
              logger = logger,
              cwd = scopePath / os.up,
              maybeRecoverOnError = maybeRecoverOnError
            )
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
