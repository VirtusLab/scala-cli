package scala.build.preprocessing

import dependency.AnyDependency
import dependency.parser.DependencyParser

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.directives.{
  HasBuildOptions,
  HasBuildOptionsWithRequirements,
  HasBuildRequirements
}
import scala.build.errors.*
import scala.build.input.{Inputs, ScalaCliInvokeData, ScalaFile, SingleElement, VirtualScalaFile}
import scala.build.internal.Util
import scala.build.options.*
import scala.build.preprocessing.directives
import scala.build.preprocessing.directives.PreprocessedDirectives
import scala.build.{Logger, Position, Positioned}
import scala.cli.directivehandler.{
  DirectiveException,
  DirectiveHandler,
  ExtractedDirectives,
  ScopePath,
  Scoped
}

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
    maybeRecoverOnDirectiveError: DirectiveException => Option[DirectiveException] = e => Some(e),
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  )(using ScalaCliInvokeData): Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case f: ScalaFile =>
        val res = either {
          val content   = value(PreprocessingUtil.maybeRead(f.path))
          val scopePath = ScopePath.fromPath(f.path)
          val source =
            value(
              process(
                content,
                Right(f.path),
                scopePath / os.up,
                logger,
                maybeRecoverOnDirectiveError,
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
                  code = updatedCode,
                  ignoreLen = 0,
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
                maybeRecoverOnDirectiveError,
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
            updatedContentOpt.getOrElse(content),
            ignoreLen = 0,
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
    maybeRecoverOnDirectiveError: DirectiveException => Option[DirectiveException],
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  )(using ScalaCliInvokeData): Either[BuildException, Option[ProcessingOutput]] = either {
    val (contentWithNoShebang, _) = SheBang.ignoreSheBangLines(content)
    val extractedDirectives: ExtractedDirectives = value(ExtractedDirectives.from(
      contentWithNoShebang.toCharArray,
      path,
      maybeRecoverOnDirectiveError
    ).left.map(new BuildDirectiveException(_)))
    value {
      processSources(
        content,
        extractedDirectives,
        path,
        scopeRoot,
        logger,
        allowRestrictedFeatures,
        suppressWarningOptions,
        maybeRecoverOnDirectiveError
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
    maybeRecoverOnDirectiveError: DirectiveException => Option[DirectiveException]
  )(using ScalaCliInvokeData): Either[BuildException, Option[ProcessingOutput]] = either {
    val (content0, isSheBang) = SheBang.ignoreSheBangLines(content)
    val preprocessedDirectives: PreprocessedDirectives =
      value(DirectivesPreprocessor.preprocess(
        extractedDirectives,
        path,
        scopeRoot,
        logger,
        allowRestrictedFeatures,
        suppressWarningOptions,
        maybeRecoverOnDirectiveError
      ))

    if (preprocessedDirectives.isEmpty) None
    else {
      val allRequirements    = Seq(preprocessedDirectives.globalReqs)
      val summedRequirements = allRequirements.foldLeft(BuildRequirements())(_ orElse _)
      val allOptions         = Seq(preprocessedDirectives.globalUsings)
      val summedOptions      = allOptions.foldLeft(BuildOptions())(_ orElse _)
      val lastContentOpt = preprocessedDirectives.strippedContent
        .orElse(if (isSheBang) Some(content0) else None)
      val directivesPositions = preprocessedDirectives.directivesPositions

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
