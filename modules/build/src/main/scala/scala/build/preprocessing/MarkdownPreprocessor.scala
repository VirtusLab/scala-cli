package scala.build.preprocessing

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.input.Inputs
import scala.build.internal.markdown.MarkdownCodeWrapper
import scala.build.internal.{AmmUtil, CodeWrapper, CustomCodeWrapper, Name}
import scala.build.options.{BuildOptions, BuildRequirements}
import scala.build.preprocessing.ScalaPreprocessor.ProcessingOutput

case object MarkdownPreprocessor extends Preprocessor {
  def preprocess(
    input: Inputs.SingleElement,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException],
    allowRestrictedFeatures: Boolean
  ): Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case markdown: Inputs.MarkdownFile =>
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
      maybeCode: Option[String],
      generatedSourceNameSuffix: String
    ): Either[BuildException, Option[PreprocessedSource.InMemory]] =
      either {
        maybeCode
          .map { code =>
            val processingOutput =
              value {
                ScalaPreprocessor.process(
                  content = code,
                  path = reportingPath,
                  scopeRoot = scopePath / os.up,
                  logger = logger,
                  maybeRecoverOnError = maybeRecoverOnError,
                  allowRestrictedFeatures = allowRestrictedFeatures
                )
              }.getOrElse(ProcessingOutput(BuildRequirements(), Nil, BuildOptions(), None))
            val processedCode = processingOutput.updatedContent.getOrElse(code)
            PreprocessedSource.InMemory(
              originalPath = reportingPath.map(subPath -> _),
              relPath = os.rel / (subPath / os.up) / s"${subPath.last}$generatedSourceNameSuffix",
              processedCode,
              ignoreLen = 0,
              options = Some(processingOutput.opts),
              requirements = Some(processingOutput.globalReqs),
              processingOutput.scopedReqs,
              mainClassOpt = None,
              scopePath = scopePath
            )
          }
      }

    val (mainScalaCode, rawScalaCode, testScalaCode) =
      value(MarkdownCodeWrapper(subPath, content, maybeRecoverOnError))

    val maybeMainFile = value(preprocessSnippets(mainScalaCode, ".scala"))
    val maybeRawFile  = value(preprocessSnippets(rawScalaCode, ".raw.scala"))
    val maybeTestFile = value(preprocessSnippets(testScalaCode, ".test.scala"))

    maybeMainFile.toList ++ maybeTestFile ++ maybeRawFile
  }

}
