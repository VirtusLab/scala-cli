package scala.build.preprocessing
import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.errors.*
import scala.build.input.{ScalaCliInvokeData, ScalaFile, SingleElement, VirtualScalaFile}
import scala.build.options.*
import scala.build.preprocessing.directives.PreprocessedDirectives
import scala.build.{Logger, Position}

case object ScalaPreprocessor extends Preprocessor {
  case class ProcessingOutput(
    globalReqs: BuildRequirements,
    scopedReqs: Seq[Scoped[BuildRequirements]],
    opts: BuildOptions,
    optsWithReqs: List[WithBuildRequirements[BuildOptions]],
    updatedContent: Option[String],
    directivesPositions: Option[Position.File]
  )

  object ProcessingOutput {
    def empty: ProcessingOutput = ProcessingOutput(
      BuildRequirements(),
      Nil,
      BuildOptions(),
      List.empty,
      None,
      None
    )
  }

  def preprocess(
    input: SingleElement,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e),
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  )(using ScalaCliInvokeData): Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case f: ScalaFile =>
        val res = either {
          val content   = value(PreprocessingUtil.maybeRead(f.path))
          val scopePath = ScopePath.fromPath(f.path)
          val source    =
            value(
              process(
                content,
                Right(f.path),
                scopePath / os.up,
                logger,
                maybeRecoverOnError,
                allowRestrictedFeatures,
                suppressWarningOptions
              )
            ) match {
              case None =>
                PreprocessedSource.OnDisk(f.path, None, List.empty, None, Nil, None, None)
              case Some(ProcessingOutput(
                    requirements,
                    scopedRequirements,
                    options,
                    optionsWithReqs,
                    Some(updatedCode),
                    directivesPositions
                  )) =>
                PreprocessedSource.InMemory(
                  originalPath = Right((f.subPath, f.path)),
                  relPath = f.subPath,
                  content = updatedCode.getBytes(StandardCharsets.UTF_8),
                  wrapperParamsOpt = None,
                  options = Some(options),
                  optionsWithTargetRequirements = optionsWithReqs,
                  requirements = Some(requirements),
                  scopedRequirements = scopedRequirements,
                  mainClassOpt = None,
                  scopePath = scopePath,
                  directivesPositions = directivesPositions
                )
              case Some(ProcessingOutput(
                    requirements,
                    scopedRequirements,
                    options,
                    optionsWithReqs,
                    None,
                    directivesPositions
                  )) =>
                PreprocessedSource.OnDisk(
                  path = f.path,
                  options = Some(options),
                  optionsWithTargetRequirements = optionsWithReqs,
                  requirements = Some(requirements),
                  scopedRequirements = scopedRequirements,
                  mainClassOpt = None,
                  directivesPositions = directivesPositions
                )
            }
          Seq(source)
        }
        Some(res)

      case v: VirtualScalaFile =>
        val res = either {
          val relPath = v match {
            case v if !v.isStdin && !v.isSnippet => v.subPath
            case v                               => os.sub / v.generatedSourceFileName
          }
          val content = new String(v.content, StandardCharsets.UTF_8)
          val (
            requirements: BuildRequirements,
            scopedRequirements: Seq[Scoped[BuildRequirements]],
            options: BuildOptions,
            optionsWithTargetRequirements: List[WithBuildRequirements[BuildOptions]],
            updatedContentOpt: Option[String],
            directivesPositions: Option[Position.File]
          ) =
            value(
              process(
                content,
                Left(v.source),
                v.scopePath / os.up,
                logger,
                maybeRecoverOnError,
                allowRestrictedFeatures,
                suppressWarningOptions
              )
            ).map {
              case ProcessingOutput(
                    reqs,
                    scopedReqs,
                    opts,
                    optsWithReqs,
                    updatedContent,
                    dirsPositions
                  ) =>
                (reqs, scopedReqs, opts, optsWithReqs, updatedContent, dirsPositions)
            }.getOrElse((
              BuildRequirements(),
              Nil,
              BuildOptions(),
              List(WithBuildRequirements(BuildRequirements(), BuildOptions())),
              None,
              None
            ))
          val s = PreprocessedSource.InMemory(
            originalPath = Left(v.source),
            relPath = relPath,
            updatedContentOpt.map(_.getBytes(StandardCharsets.UTF_8)).getOrElse(v.content),
            wrapperParamsOpt = None,
            options = Some(options),
            optionsWithTargetRequirements = optionsWithTargetRequirements,
            requirements = Some(requirements),
            scopedRequirements,
            mainClassOpt = None,
            scopePath = v.scopePath,
            directivesPositions = directivesPositions
          )
          Seq(s)
        }
        Some(res)

      case _ =>
        None
    }

  def process(
    content: String,
    path: Either[String, os.Path],
    scopeRoot: ScopePath,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException],
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  )(using ScalaCliInvokeData): Either[BuildException, Option[ProcessingOutput]] = either {
    val (contentWithNoShebang, _)                = SheBang.ignoreSheBangLines(content)
    val extractedDirectives: ExtractedDirectives = value(ExtractedDirectives.from(
      contentChars = contentWithNoShebang.toCharArray,
      path = path,
      suppressWarningOptions = suppressWarningOptions,
      logger = logger,
      maybeRecoverOnError = maybeRecoverOnError
    ))
    value {
      processSources(
        content,
        extractedDirectives,
        path,
        scopeRoot,
        logger,
        allowRestrictedFeatures,
        suppressWarningOptions,
        maybeRecoverOnError
      )
    }
  }

  def processSources(
    content: String,
    extractedDirectives: ExtractedDirectives,
    path: Either[String, os.Path],
    scopeRoot: ScopePath,
    logger: Logger,
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions,
    maybeRecoverOnError: BuildException => Option[BuildException]
  )(using ScalaCliInvokeData): Either[BuildException, Option[ProcessingOutput]] = either {
    DeprecatedDirectives.issueWarnings(
      path,
      extractedDirectives.directives,
      suppressWarningOptions,
      logger
    )

    val (content0, isSheBang)                          = SheBang.ignoreSheBangLines(content)
    val preprocessedDirectives: PreprocessedDirectives =
      value(DirectivesPreprocessor(
        path,
        scopeRoot,
        logger,
        allowRestrictedFeatures,
        suppressWarningOptions,
        maybeRecoverOnError
      ).preprocess(
        extractedDirectives
      ))

    if (preprocessedDirectives.isEmpty) None
    else {
      val allRequirements    = Seq(preprocessedDirectives.globalReqs)
      val summedRequirements = allRequirements.foldLeft(BuildRequirements())(_ orElse _)
      val allOptions         = Seq(preprocessedDirectives.globalUsings)
      val summedOptions      = allOptions.foldLeft(BuildOptions())(_ orElse _)
      val lastContentOpt     = preprocessedDirectives.strippedContent
        .orElse(if (isSheBang) Some(content0) else None)
      val directivesPositions = preprocessedDirectives.directivesPositions.map { pos =>
        if (isSheBang) pos.copy(endPos = pos.endPos._1 + 1 -> pos.endPos._2) else pos
      }

      val scopedRequirements = preprocessedDirectives.scopedReqs
      Some(ProcessingOutput(
        summedRequirements,
        scopedRequirements,
        summedOptions,
        preprocessedDirectives.usingsWithReqs,
        lastContentOpt,
        directivesPositions
      ))
    }
  }
}
