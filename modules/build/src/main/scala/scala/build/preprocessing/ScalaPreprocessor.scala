package scala.build.preprocessing

import com.virtuslab.using_directives.custom.model.UsingDirectiveKind
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
import scala.build.input.{Inputs, ScalaFile, SingleElement, VirtualScalaFile}
import scala.build.internal.Util
import scala.build.options.*
import scala.build.preprocessing.DirectivesProcessor.DirectivesProcessorOutput
import scala.build.preprocessing.directives
import scala.build.preprocessing.directives.{DirectiveHandler, DirectiveUtil, ScopedDirective}
import scala.build.{Logger, Position, Positioned}

case object ScalaPreprocessor extends Preprocessor {

  private case class StrictDirectivesProcessingOutput(
    globalReqs: BuildRequirements,
    globalUsings: BuildOptions,
    usingsWithReqs: List[WithBuildRequirements[BuildOptions]],
    scopedReqs: Seq[Scoped[BuildRequirements]],
    strippedContent: Option[String],
    directivesPositions: Option[DirectivesPositions]
  ) {
    def isEmpty: Boolean = globalReqs == BuildRequirements.monoid.zero &&
      globalUsings == BuildOptions.monoid.zero &&
      scopedReqs.isEmpty &&
      strippedContent.isEmpty
  }

  private case class SpecialImportsProcessingOutput(
    reqs: BuildRequirements,
    opts: BuildOptions,
    content: String
  )

  case class ProcessingOutput(
    globalReqs: BuildRequirements,
    scopedReqs: Seq[Scoped[BuildRequirements]],
    opts: BuildOptions,
    optsWithReqs: List[WithBuildRequirements[BuildOptions]],
    updatedContent: Option[String],
    directivesPositions: Option[DirectivesPositions]
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

  val usingDirectiveHandlers: Seq[DirectiveHandler[BuildOptions]] =
    Seq[DirectiveHandler[_ <: HasBuildOptions]](
      directives.CustomJar.handler,
      directives.JavaHome.handler,
      directives.Jvm.handler,
      directives.MainClass.handler,
      directives.Packaging.handler,
      directives.Platform.handler,
      directives.Plugin.handler,
      directives.Publish.handler,
      directives.PublishContextual.Local.handler,
      directives.PublishContextual.CI.handler,
      directives.Python.handler,
      directives.Repository.handler,
      directives.ScalaJs.handler,
      directives.ScalaNative.handler,
      directives.ScalaVersion.handler,
      directives.Sources.handler,
      directives.Tests.handler,
      directives.Toolkit.handler
    ).map(_.mapE(_.buildOptions))

  val usingDirectiveWithReqsHandlers
    : Seq[DirectiveHandler[List[WithBuildRequirements[BuildOptions]]]] =
    Seq[DirectiveHandler[_ <: HasBuildOptionsWithRequirements]](
      directives.Dependency.handler,
      directives.JavaOptions.handler,
      directives.JavacOptions.handler,
      directives.JavaProps.handler,
      directives.Resources.handler,
      directives.ScalacOptions.handler
    ).map(_.mapE(_.buildOptionsWithRequirements))

  val requireDirectiveHandlers: Seq[DirectiveHandler[BuildRequirements]] =
    Seq[DirectiveHandler[_ <: HasBuildRequirements]](
      directives.RequirePlatform.handler,
      directives.RequireScalaVersion.handler,
      directives.RequireScalaVersionBounds.handler,
      directives.RequireScope.handler
    ).map(_.mapE(_.buildRequirements))

  def preprocess(
    input: SingleElement,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e),
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  ): Option[Either[BuildException, Seq[PreprocessedSource]]] =
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
            directivesPositions: Option[DirectivesPositions]
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
    maybeRecoverOnError: BuildException => Option[BuildException],
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  ): Either[BuildException, Option[ProcessingOutput]] = either {
    val (contentWithNoShebang, _) = SheBang.ignoreSheBangLines(content)
    val extractedDirectives: ExtractedDirectives = value(ExtractedDirectives.from(
      contentWithNoShebang.toCharArray,
      path,
      logger,
      scopeRoot,
      maybeRecoverOnError
    ))
    value(processSources(
      content,
      extractedDirectives,
      path,
      scopeRoot,
      logger,
      allowRestrictedFeatures,
      suppressWarningOptions
    ))
  }

  def processSources(
    content: String,
    extractedDirectives: ExtractedDirectives,
    path: Either[String, os.Path],
    scopeRoot: ScopePath,
    logger: Logger,
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  ): Either[BuildException, Option[ProcessingOutput]] = either {
    val (content0, isSheBang) = SheBang.ignoreSheBangLines(content)
    val afterStrictUsing: StrictDirectivesProcessingOutput =
      value(processStrictUsing(
        content0,
        extractedDirectives,
        path,
        scopeRoot,
        logger,
        allowRestrictedFeatures,
        suppressWarningOptions
      ))

    if (afterStrictUsing.isEmpty) None
    else {
      val allRequirements    = Seq(afterStrictUsing.globalReqs)
      val summedRequirements = allRequirements.foldLeft(BuildRequirements())(_ orElse _)
      val allOptions         = Seq(afterStrictUsing.globalUsings)
      val summedOptions      = allOptions.foldLeft(BuildOptions())(_ orElse _)
      val lastContentOpt = afterStrictUsing.strippedContent
        .orElse(if (isSheBang) Some(content0) else None)
      val directivesPositions = afterStrictUsing.directivesPositions

      val scopedRequirements = afterStrictUsing.scopedReqs
      Some(ProcessingOutput(
        summedRequirements,
        scopedRequirements,
        summedOptions,
        afterStrictUsing.usingsWithReqs,
        lastContentOpt,
        directivesPositions
      ))
    }
  }

  private def processStrictUsing(
    content: String,
    extractedDirectives: ExtractedDirectives,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger,
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  ): Either[BuildException, StrictDirectivesProcessingOutput] = either {
    val contentChars = content.toCharArray

    val ExtractedDirectives(directives0, directivesPositions) = extractedDirectives

    val updatedOptions: DirectivesProcessorOutput[BuildOptions] = value {
      DirectivesProcessor.process(
        directives0,
        usingDirectiveHandlers,
        path,
        cwd,
        logger,
        allowRestrictedFeatures,
        suppressWarningOptions
      )
    }

    val directives1 = updatedOptions.unused

    val optionsWithTargetRequirements
      : DirectivesProcessorOutput[List[WithBuildRequirements[BuildOptions]]] = value {
      DirectivesProcessor.process(
        directives1,
        usingDirectiveWithReqsHandlers,
        path,
        cwd,
        logger,
        allowRestrictedFeatures,
        suppressWarningOptions
      )
    }

    val directives2 = optionsWithTargetRequirements.unused

    val updatedRequirements = value {
      DirectivesProcessor.process(
        directives2,
        requireDirectiveHandlers,
        path,
        cwd,
        logger,
        allowRestrictedFeatures,
        suppressWarningOptions
      )
    }

    val unusedDirectives = updatedRequirements.unused

    val (optionsWithActualRequirements, optionsWithEmptyRequirements) =
      optionsWithTargetRequirements.global.partition(_.requirements.nonEmpty)
    val summedOptionsWithNoRequirements =
      optionsWithEmptyRequirements
        .map(_.value)
        .foldLeft(updatedOptions.global)((acc, bo) => acc.orElse(bo))

    value {
      unusedDirectives match {
        case Seq() =>
          Right(StrictDirectivesProcessingOutput(
            updatedRequirements.global,
            summedOptionsWithNoRequirements,
            optionsWithActualRequirements,
            updatedRequirements.scoped,
            strippedContent = None,
            directivesPositions
          ))
        case Seq(h, t*) =>
          val errors = ::(
            handleUnusedValues(ScopedDirective(h, path, cwd)),
            t.map(d => handleUnusedValues(ScopedDirective(d, path, cwd))).toList
          )
          Left(CompositeBuildException(errors))
      }
    }
  }

  private def handleUnusedValues(
    scopedDirective: ScopedDirective
  ): BuildException = {
    val values =
      DirectiveUtil.concatAllValues(scopedDirective)
    new UnusedDirectiveError(
      scopedDirective.directive.key,
      values.map(_.value),
      values.flatMap(_.positions)
    )
  }

  private def parseDependency(str: String, pos: Position): Either[BuildException, AnyDependency] =
    DependencyParser.parse(str) match {
      case Left(msg)  => Left(new DependencyFormatError(str, msg, positionOpt = Some(pos)))
      case Right(dep) => Right(dep)
    }
}
