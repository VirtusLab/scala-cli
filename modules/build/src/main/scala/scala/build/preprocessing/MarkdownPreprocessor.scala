package scala.build.preprocessing

import coursier.cache.ArchiveCache
import coursier.util.Task

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.input.{MarkdownFile, ScalaCliInvokeData, SingleElement, VirtualMarkdownFile}
import scala.build.internal.JavaParserProxyMaker
import scala.build.internal.markdown.{MarkdownCodeBlock, MarkdownCodeWrapper}
import scala.build.options.SuppressWarningOptions
import scala.build.preprocessing.ScalaPreprocessor.ProcessingOutput
import scala.build.preprocessing.directives.PreprocessedDirectives

/** Markdown source preprocessor.
  *
  * @param archiveCache
  *   when using a java-class-name external binary to infer a class name (see [[JavaParserProxy]]),
  *   a cache to download that binary with
  * @param javaClassNameVersionOpt
  *   when using a java-class-name external binary to infer a class name (see [[JavaParserProxy]]),
  *   this forces the java-class-name version to download
  */
final case class MarkdownPreprocessor(
  archiveCache: ArchiveCache[Task],
  javaClassNameVersionOpt: Option[String],
  javaCommand: () => String
) extends Preprocessor {
  def preprocess(
    input: SingleElement,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException],
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  )(using ScalaCliInvokeData): Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case markdown: MarkdownFile =>
        val res = either {
          val content      = value(PreprocessingUtil.maybeRead(markdown.path))
          val preprocessed = value {
            preprocessContent(
              archiveCache = archiveCache,
              javaClassNameVersionOpt = javaClassNameVersionOpt,
              javaCommand = javaCommand,
              reportingPath = Right(markdown.path),
              content = content,
              subPath = markdown.subPath,
              scopePath = ScopePath.fromPath(markdown.path),
              logger = logger,
              maybeRecoverOnError = maybeRecoverOnError,
              allowRestrictedFeatures = allowRestrictedFeatures,
              suppressWarningOptions = suppressWarningOptions
            )
          }
          preprocessed
        }
        Some(res)
      case markdown: VirtualMarkdownFile =>
        val content = new String(markdown.content, StandardCharsets.UTF_8)
        val res     = either {
          val preprocessed = value {
            preprocessContent(
              archiveCache = archiveCache,
              javaClassNameVersionOpt = javaClassNameVersionOpt,
              javaCommand = javaCommand,
              reportingPath = Left(markdown.source),
              content = content,
              subPath = markdown.wrapperPath,
              scopePath = markdown.scopePath,
              logger = logger,
              maybeRecoverOnError = maybeRecoverOnError,
              allowRestrictedFeatures = allowRestrictedFeatures,
              suppressWarningOptions = suppressWarningOptions
            )
          }
          preprocessed
        }
        Some(res)
      case _ =>
        None
    }

  private def preprocessContent(
    archiveCache: ArchiveCache[Task],
    javaClassNameVersionOpt: Option[String],
    javaCommand: () => String,
    reportingPath: Either[String, os.Path],
    content: String,
    subPath: os.SubPath,
    scopePath: ScopePath,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException],
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  )(using ScalaCliInvokeData): Either[BuildException, List[PreprocessedSource.InMemory]] = either {
    def preprocessSnippets(
      maybeWrapper: Option[MarkdownCodeWrapper.WrappedMarkdownCode],
      generatedSourceNameSuffix: String
    ): Either[BuildException, Option[PreprocessedSource.InMemory]] =
      either {
        maybeWrapper
          .map { wrappedMarkdown =>
            val processingOutput: ProcessingOutput =
              value {
                ScalaPreprocessor.processSources(
                  content = wrappedMarkdown.code,
                  extractedDirectives = wrappedMarkdown.directives,
                  path = reportingPath,
                  scopeRoot = scopePath / os.up,
                  logger = logger,
                  allowRestrictedFeatures = allowRestrictedFeatures,
                  suppressWarningOptions = suppressWarningOptions,
                  maybeRecoverOnError = maybeRecoverOnError
                )
              }.getOrElse(ProcessingOutput.empty)
            val processedCode = processingOutput.updatedContent.getOrElse(wrappedMarkdown.code)
            PreprocessedSource.InMemory(
              originalPath = reportingPath.map(subPath -> _),
              relPath = os.rel / (subPath / os.up) / s"${subPath.last}$generatedSourceNameSuffix",
              processedCode.getBytes(StandardCharsets.UTF_8),
              wrapperParamsOpt = None,
              options = Some(processingOutput.opts),
              optionsWithTargetRequirements = processingOutput.optsWithReqs,
              requirements = Some(processingOutput.globalReqs),
              processingOutput.scopedReqs,
              mainClassOpt = None,
              scopePath = scopePath,
              directivesPositions = processingOutput.directivesPositions
            )
          }
      }

    def emitJavaSnippets(
      blocks: PreprocessedMarkdownCodeBlocks,
      isTest: Boolean
    ): Either[BuildException, List[PreprocessedSource.InMemory]] =
      either {
        val javaParser =
          (new JavaParserProxyMaker)
            .get(
              archiveCache,
              javaClassNameVersionOpt,
              logger,
              () => javaCommand()
            )
        blocks.codeBlocks.zipWithIndex.map { (block, index) =>
          value {
            emitJavaSnippet(
              block = block,
              index = index,
              isTest = isTest,
              subPath = subPath,
              scopePath = scopePath,
              reportingPath = reportingPath,
              javaParser = javaParser,
              logger = logger,
              allowRestrictedFeatures = allowRestrictedFeatures,
              suppressWarningOptions = suppressWarningOptions,
              maybeRecoverOnError = maybeRecoverOnError
            )
          }
        }.toList
      }

    val codeBlocks: Seq[MarkdownCodeBlock] =
      value(MarkdownCodeBlock.findCodeBlocks(subPath, content, maybeRecoverOnError))
    val preprocessedMarkdown: PreprocessedMarkdown =
      value(MarkdownCodeBlockProcessor.process(
        codeBlocks,
        reportingPath,
        suppressWarningOptions,
        logger,
        maybeRecoverOnError
      ))

    val (mainScalaCode, rawScalaCode, testScalaCode) =
      MarkdownCodeWrapper(subPath, preprocessedMarkdown)

    val maybeMainFile = value(preprocessSnippets(mainScalaCode, ".scala"))
    val maybeRawFile  = value(preprocessSnippets(rawScalaCode, ".raw.scala"))
    val maybeTestFile = value(preprocessSnippets(testScalaCode, ".test.scala"))
    val javaFiles     = value(emitJavaSnippets(preprocessedMarkdown.javaCodeBlocks, isTest = false))
    val javaTestFiles =
      value(emitJavaSnippets(preprocessedMarkdown.javaTestCodeBlocks, isTest = true))

    maybeMainFile.toList ++ maybeTestFile ++ maybeRawFile ++ javaFiles ++ javaTestFiles
  }

  private def emitJavaSnippet(
    block: MarkdownCodeBlock,
    index: Int,
    isTest: Boolean,
    subPath: os.SubPath,
    scopePath: ScopePath,
    reportingPath: Either[String, os.Path],
    javaParser: scala.build.internal.JavaParserProxy,
    logger: Logger,
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions,
    maybeRecoverOnError: BuildException => Option[BuildException]
  )(using ScalaCliInvokeData): Either[BuildException, PreprocessedSource.InMemory] = either {
    val classNameOpt = value {
      javaParser.className(block.body.getBytes(StandardCharsets.UTF_8))
    }
    val mdBaseName   = subPath.last.stripSuffix(".md")
    val baseName     = classNameOpt.getOrElse(s"${mdBaseName}_md_snippet$index")
    val javaFileName =
      if isTest then s"$baseName.test.java"
      else s"$baseName.java"
    val generatedRelPath =
      if isTest then os.rel / (subPath / os.up) / s"$baseName.java"
      else os.rel / (subPath / os.up) / javaFileName
    val snippetScopePath = scopePath.copy(subPath = (subPath / os.up) / javaFileName)
    val preprocessedDirectives: PreprocessedDirectives = value {
      DirectivesPreprocessor(
        reportingPath,
        snippetScopePath,
        logger,
        allowRestrictedFeatures,
        suppressWarningOptions,
        maybeRecoverOnError
      ).preprocess(block.body)
    }
    PreprocessedSource.InMemory(
      originalPath = reportingPath.map(subPath -> _),
      relPath = generatedRelPath,
      content = block.body.getBytes(StandardCharsets.UTF_8),
      wrapperParamsOpt = None,
      options = Some(preprocessedDirectives.globalUsings),
      optionsWithTargetRequirements = preprocessedDirectives.usingsWithReqs,
      requirements = Some(preprocessedDirectives.globalReqs),
      scopedRequirements = preprocessedDirectives.scopedReqs,
      mainClassOpt = None,
      scopePath = snippetScopePath,
      directivesPositions = preprocessedDirectives.directivesPositions
    )
  }
}
