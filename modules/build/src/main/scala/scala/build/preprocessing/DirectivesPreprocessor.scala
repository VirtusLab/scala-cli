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
  DirectiveErrors,
  UnusedDirectiveError
}
import scala.build.internal.util.WarningMessages.experimentalDirectiveUsed
import scala.build.options.{
  BuildOptions,
  BuildRequirements,
  ConfigMonoid,
  SuppressWarningOptions,
  WithBuildRequirements
}
import scala.build.preprocessing.directives.DirectivesPreprocessingUtils.*
import scala.build.preprocessing.directives._

object DirectivesPreprocessor {
  def preprocess(
    content: String,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger,
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e)
  ): Either[BuildException, PreprocessedDirectives] = either {
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
        suppressWarningOptions
      )
    }
  }

  def preprocess(
    extractedDirectives: ExtractedDirectives,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger,
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  ): Either[BuildException, PreprocessedDirectives] = either {
    val ExtractedDirectives(directives0, directivesPositions) = extractedDirectives

    val updatedOptions: PartiallyProcessedDirectives[BuildOptions] = value {
      DirectivesPreprocessor.applyDirectiveHandlers(
        directives0,
        usingDirectiveHandlers,
        path,
        cwd,
        logger,
        allowRestrictedFeatures,
        suppressWarningOptions
      )
    }

    val optionsWithTargetRequirements
      : PartiallyProcessedDirectives[List[WithBuildRequirements[BuildOptions]]] =
      updatedOptions.andThen { directives1 =>
        value {
          DirectivesPreprocessor.applyDirectiveHandlers(
            directives1,
            usingDirectiveWithReqsHandlers,
            path,
            cwd,
            logger,
            allowRestrictedFeatures,
            suppressWarningOptions
          )
        }
      }

    val updatedRequirements: PartiallyProcessedDirectives[BuildRequirements] =
      optionsWithTargetRequirements.andThen { directives2 =>
        value {
          DirectivesPreprocessor.applyDirectiveHandlers(
            directives2,
            requireDirectiveHandlers,
            path,
            cwd,
            logger,
            allowRestrictedFeatures,
            suppressWarningOptions
          )
        }
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
          Right(PreprocessedDirectives(
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

  private def applyDirectiveHandlers[T: ConfigMonoid](
    directives: Seq[StrictDirective],
    handlers: Seq[DirectiveHandler[T]],
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger,
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  ): Either[BuildException, PartiallyProcessedDirectives[T]] = {
    val configMonoidInstance = implicitly[ConfigMonoid[T]]
    val shouldSuppressExperimentalFeatures =
      suppressWarningOptions.suppressExperimentalFeatureWarning.getOrElse(false)

    def handleValues(handler: DirectiveHandler[T])(
      scopedDirective: ScopedDirective,
      logger: Logger
    ) =
      if !allowRestrictedFeatures && (handler.isRestricted || handler.isExperimental) then
        val powerDirectiveType = if handler.isExperimental then "experimental" else "restricted"
        val msg = // TODO pass the called progName here to print the full config command
          s"""The '${scopedDirective.directive.toString}' directive is $powerDirectiveType.
             |Please run it with the '--power' flag or turn or turn power mode on globally by running:
             |  ${Console.BOLD}config power true${Console.RESET}""".stripMargin
        Left(DirectiveErrors(
          ::(msg, Nil),
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
