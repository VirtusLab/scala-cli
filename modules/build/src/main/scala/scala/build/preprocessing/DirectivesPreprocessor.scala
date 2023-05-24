package scala.build.preprocessing
import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.Ops.*
import scala.build.directives.{
  HasBuildOptions,
  HasBuildOptionsWithRequirements,
  HasBuildRequirements
}
import scala.build.errors.{
  BuildException,
  CompositeBuildException,
  DirectiveErrors
}
import scala.build.input.ScalaCliInvokeData
import scala.build.internal.util.WarningMessages
import scala.build.internal.util.WarningMessages.experimentalDirectiveUsed
import scala.build.options.{
  BuildOptions,
  BuildRequirements,
  ConfigMonoid,
  SuppressWarningOptions,
  WithBuildRequirements
}
import scala.build.preprocessing.directives.DirectivesPreprocessingUtils.*
import scala.build.preprocessing.directives.PartiallyProcessedDirectives.*
import scala.build.preprocessing.directives.*

object DirectivesPreprocessor {
  def preprocess(
    content: String,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger,
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions,
    maybeRecoverOnError: BuildException => Option[BuildException]
  )(using ScalaCliInvokeData): Either[BuildException, PreprocessedDirectives] = either {
    val directives = value {
      ExtractedDirectives.from(content.toCharArray, path, logger, maybeRecoverOnError)
    }
    value {
      preprocess(
        directives,
        path,
        cwd,
        logger,
        allowRestrictedFeatures,
        suppressWarningOptions,
        maybeRecoverOnError
      )
    }
  }

  def preprocess(
    extractedDirectives: ExtractedDirectives,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger,
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions,
    maybeRecoverOnError: BuildException => Option[BuildException]
  )(using ScalaCliInvokeData): Either[BuildException, PreprocessedDirectives] = either {
    val ExtractedDirectives(directives, directivesPositions) = extractedDirectives
    def preprocessWithDirectiveHandlers[T: ConfigMonoid](
      remainingDirectives: Seq[StrictDirective],
      directiveHandlers: Seq[DirectiveHandler[T]]
    ): Either[BuildException, PartiallyProcessedDirectives[T]] =
      applyDirectiveHandlers(
        remainingDirectives,
        directiveHandlers,
        path,
        cwd,
        logger,
        allowRestrictedFeatures,
        suppressWarningOptions,
        maybeRecoverOnError
      )

    val (
      buildOptionsWithoutRequirements: PartiallyProcessedDirectives[BuildOptions],
      buildOptionsWithTargetRequirements: PartiallyProcessedDirectives[
        List[WithBuildRequirements[BuildOptions]]
      ],
      scopedBuildRequirements: PartiallyProcessedDirectives[BuildRequirements],
      unusedDirectives: Seq[StrictDirective]
    ) = value {
      for {
        regularUsingDirectives: PartiallyProcessedDirectives[BuildOptions] <-
          preprocessWithDirectiveHandlers(directives, usingDirectiveHandlers)
        usingDirectivesWithRequirements: PartiallyProcessedDirectives[
          List[WithBuildRequirements[BuildOptions]]
        ] <-
          preprocessWithDirectiveHandlers(
            regularUsingDirectives.unused,
            usingDirectiveWithReqsHandlers
          )
        targetDirectives: PartiallyProcessedDirectives[BuildRequirements] <-
          preprocessWithDirectiveHandlers(
            usingDirectivesWithRequirements.unused,
            requireDirectiveHandlers
          )
        remainingDirectives = targetDirectives.unused
      } yield (
        regularUsingDirectives,
        usingDirectivesWithRequirements,
        targetDirectives,
        remainingDirectives
      )
    }

    val (optionsWithActualRequirements, optionsWithEmptyRequirements) =
      buildOptionsWithTargetRequirements.global.partition(_.requirements.nonEmpty)
    val summedOptionsWithNoRequirements =
      optionsWithEmptyRequirements
        .map(_.value)
        .foldLeft(buildOptionsWithoutRequirements.global)((acc, bo) => acc.orElse(bo))

    value {
      unusedDirectives.toList match {
        case Nil =>
          Right {
            PreprocessedDirectives(
              scopedBuildRequirements.global,
              summedOptionsWithNoRequirements,
              optionsWithActualRequirements,
              scopedBuildRequirements.scoped,
              strippedContent = None,
              directivesPositions
            )
          }
        case unused =>
          maybeRecoverOnError {
            CompositeBuildException(
              exceptions = unused.map(ScopedDirective(_, path, cwd).unusedDirectiveError)
            )
          }.toLeft(PreprocessedDirectives.empty)
      }
    }
  }

  private def applyDirectiveHandlers[T: ConfigMonoid](
    directives: Seq[StrictDirective],
    handlers: Seq[DirectiveHandler[T]],
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger,
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e)
  )(using ScalaCliInvokeData): Either[BuildException, PartiallyProcessedDirectives[T]] = {
    val configMonoidInstance = implicitly[ConfigMonoid[T]]
    val shouldSuppressExperimentalFeatures =
      suppressWarningOptions.suppressExperimentalFeatureWarning.getOrElse(false)

    def handleValues(handler: DirectiveHandler[T])(
      scopedDirective: ScopedDirective,
      logger: Logger
    ): Either[BuildException, ProcessedDirective[T]] =
      if !allowRestrictedFeatures && (handler.isRestricted || handler.isExperimental) then
        Left(DirectiveErrors(
          ::(WarningMessages.powerDirectiveUsedInSip(scopedDirective, handler), Nil),
          DirectiveUtil.positions(scopedDirective.directive.values, path)
        ))
      else
        if handler.isExperimental && !shouldSuppressExperimentalFeatures then
          logger.message(experimentalDirectiveUsed(scopedDirective.directive.toString))
        handler.handleValues(scopedDirective, logger)

    val handlersMap = handlers
      .flatMap { handler =>
        handler.keys.map(k => k -> handleValues(handler))
      }
      .toMap

    val unused = directives.filter(d => !handlersMap.contains(d.key))

    val res = directives
      .iterator
      .flatMap {
        case d @ StrictDirective(k, _) =>
          handlersMap.get(k).iterator.map(_(ScopedDirective(d, path, cwd), logger))
      }
      .toVector
      .flatMap {
        case Left(e: BuildException) => maybeRecoverOnError(e).toVector.map(Left(_))
        case r @ Right(_)            => Vector(r)
      }
      .sequence
      .left.map(CompositeBuildException(_))
      .map(_.foldLeft((configMonoidInstance.zero, Seq.empty[Scoped[T]])) {
        case ((globalAcc, scopedAcc), ProcessedDirective(global, scoped)) => (
            global.fold(globalAcc)(ns => configMonoidInstance.orElse(ns, globalAcc)),
            scopedAcc ++ scoped
          )
      })
    res.map {
      case (g, s) => PartiallyProcessedDirectives(g, s, unused)
    }
  }
}
