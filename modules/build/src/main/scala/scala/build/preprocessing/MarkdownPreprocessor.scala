package scala.build.preprocessing

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.input.{Inputs, MarkdownFile, SingleElement, VirtualMarkdownFile}
import scala.build.internal.markdown.{MarkdownCodeBlock, MarkdownCodeWrapper}
import scala.build.internal.{AmmUtil, CodeWrapper, CustomCodeWrapper, Name}
import scala.build.options.{BuildOptions, BuildRequirements}
import scala.build.preprocessing.ScalaPreprocessor.ProcessingOutput

case object MarkdownPreprocessor extends Preprocessor {
  def preprocess(
    input: SingleElement,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException],
    allowRestrictedFeatures: Boolean
  ): Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case markdown: MarkdownFile =>
        val res = either {
          val content = value(PreprocessingUtil.maybeRead(markdown.path))
          val preprocessed = value {
            MarkdownPreprocessor.preprocess(
              Right(markdown.path),
              content,
              markdown.subPath,
              ScopePath.fromPath(markdown.path),
              logger,
              maybeRecoverOnError,
              allowRestrictedFeatures
            )
          }
          preprocessed
        }
        Some(res)
      case markdown: VirtualMarkdownFile =>
        val content = new String(markdown.content, StandardCharsets.UTF_8)
        val res = either {
          val preprocessed = value {
            MarkdownPreprocessor.preprocess(
              Left(markdown.source),
              content,
              markdown.wrapperPath,
              markdown.scopePath,
              logger,
              maybeRecoverOnError,
              allowRestrictedFeatures
            )
          }
          preprocessed
        }
        Some(res)
      case _ =>
        None
    }

  private def preprocess(
    reportingPath: Either[String, os.Path],
    content: String,
    subPath: os.SubPath,
    scopePath: ScopePath,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException],
    allowRestrictedFeatures: Boolean
  ): Either[BuildException, List[PreprocessedSource.InMemory]] = either {
    def preprocessSnippets(
      maybeWrapper: Option[MarkdownCodeWrapper.WrappedMarkdownCode],
      generatedSourceNameSuffix: String
    ): Either[BuildException, Option[PreprocessedSource.InMemory]] =
      either {
        maybeWrapper
          .map { wrappedMarkdown =>
            val processingOutput: ProcessingOutput =
              value {
                ScalaPreprocessor.process(
                  content = wrappedMarkdown.code,
                  extractedDirectives = wrappedMarkdown.directives,
                  path = reportingPath,
                  scopeRoot = scopePath / os.up,
                  logger = logger,
                  maybeRecoverOnError = maybeRecoverOnError,
                  allowRestrictedFeatures = allowRestrictedFeatures
                )
              }.getOrElse(ProcessingOutput(BuildRequirements(), Nil, BuildOptions(), None, None))
            val processedCode = processingOutput.updatedContent.getOrElse(wrappedMarkdown.code)
            PreprocessedSource.InMemory(
              originalPath = reportingPath.map(subPath -> _),
              relPath = os.rel / (subPath / os.up) / s"${subPath.last}$generatedSourceNameSuffix",
              processedCode,
              ignoreLen = 0,
              options = Some(processingOutput.opts),
              requirements = Some(processingOutput.globalReqs),
              processingOutput.scopedReqs,
              mainClassOpt = None,
              scopePath = scopePath,
              directivesPositions = processingOutput.directivesPositions
            )
          }
      }

    val codeBlocks: Seq[MarkdownCodeBlock] =
      value(MarkdownCodeBlock.findCodeBlocks(subPath, content, maybeRecoverOnError))
    val preprocessedMarkdown: PreprocessedMarkdown =
      value(MarkdownCodeBlockProcessor.process(
        codeBlocks,
        reportingPath,
        scopePath,
        logger,
        maybeRecoverOnError
      ))

    val (mainScalaCode, rawScalaCode, testScalaCode) =
      MarkdownCodeWrapper(subPath, preprocessedMarkdown)

    val maybeMainFile = value(preprocessSnippets(mainScalaCode, ".scala"))
    val maybeRawFile  = value(preprocessSnippets(rawScalaCode, ".raw.scala"))
    val maybeTestFile = value(preprocessSnippets(testScalaCode, ".test.scala"))

    maybeMainFile.toList ++ maybeTestFile ++ maybeRawFile
  }

}
