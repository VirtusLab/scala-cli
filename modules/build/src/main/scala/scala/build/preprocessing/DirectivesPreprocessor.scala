package scala.build.preprocessing

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.Ops.*
import scala.build.directives.{
  HasBuildOptions,
  HasBuildOptionsWithRequirements,
  HasBuildRequirements
}
import scala.build.errors.{BuildException, CompositeBuildException}
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
import scala.build.preprocessing.BuildDirectiveException
import scala.build.preprocessing.directives.DirectivesPreprocessingUtils.*
import scala.build.preprocessing.directives.DirectiveUtil.{isExperimental, isRestricted, toScalaCli}
import scala.build.preprocessing.directives.PartiallyProcessedDirectives.*
import scala.build.preprocessing.directives.*
import scala.cli.directivehandler.*

object DirectivesPreprocessor {
  def preprocess(
    content: String,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger,
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions,
    maybeRecoverOnError: DirectiveException => Option[DirectiveException]
  )(using ScalaCliInvokeData): Either[BuildException, PreprocessedDirectives] = either {
    val directives = value {
      ExtractedDirectives.from(content.toCharArray, path, maybeRecoverOnError)
        .left.map(new BuildDirectiveException(_))
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
    maybeRecoverOnError: DirectiveException => Option[DirectiveException]
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
              directivesPositions.map(_.toScalaCli)
            )
          }
        case unused =>
          maybeRecoverOnError {
            CompositeDirectiveException(
              exceptions = unused.map(ScopedDirective(_, path, cwd).unusedDirectiveError)
            )
          }.map(new BuildDirectiveException(_)).toLeft(PreprocessedDirectives.empty)
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
    maybeRecoverOnError: DirectiveException => Option[DirectiveException] = e => Some(e)
  )(using ScalaCliInvokeData): Either[BuildException, PartiallyProcessedDirectives[T]] = {
    val configMonoidInstance = implicitly[ConfigMonoid[T]]
    val shouldSuppressExperimentalFeatures =
      suppressWarningOptions.suppressExperimentalFeatureWarning.getOrElse(false)

    def handleValues(handler: DirectiveHandler[T])(
      scopedDirective: ScopedDirective,
      logger: Logger
    ): Either[DirectiveException, ProcessedDirective[T]] =
      if !allowRestrictedFeatures && (handler.isRestricted || handler.isExperimental) then
        Left(DirectiveErrors(
          ::(WarningMessages.powerDirectiveUsedInSip(scopedDirective, handler), Nil),
          scala.build.preprocessing.directives.DirectiveUtil.positions(
            scopedDirective.directive.values,
            path
          )
        ))
      else
        if handler.isExperimental && !shouldSuppressExperimentalFeatures then
          logger.message(experimentalDirectiveUsed(scopedDirective.directive.toString))
        handler.handleValues(scopedDirective)

    val handlersMap = handlers
      .flatMap { handler =>
        handler.keys.map(k => k -> handleValues(handler))
      }
      .toMap

    val unused = directives.filter(d => !handlersMap.contains(d.key))

    val res0: Vector[Either[DirectiveException, ProcessedDirective[T]]] = directives
      .iterator
      .flatMap {
        case d @ StrictDirective(k, _) =>
          val a: Iterator[Either[
            cli.directivehandler.DirectiveException,
            cli.directivehandler.ProcessedDirective[T]
          ]] = handlersMap.get(k).iterator.map(_(ScopedDirective(d, path, cwd), logger))
          a
      }
      .toVector
    val res1: Vector[Either[DirectiveException, ProcessedDirective[T]]] = res0
      .flatMap {
        case Left(e: DirectiveException) => maybeRecoverOnError(e).toVector.map(e0 => Left(e0))
        case r                           => Vector(r)
      }
    val res = res1
      .sequence
      .left.map(CompositeDirectiveException(_))
      .left.map(new BuildDirectiveException(_))
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
