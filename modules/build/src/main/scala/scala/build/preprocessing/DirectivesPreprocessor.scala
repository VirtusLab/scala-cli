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
import scala.build.input.ScalaCliInvokeData
import scala.build.internal.util.WarningMessages
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

case class DirectivesPreprocessor(
  path: Either[String, os.Path],
  cwd: ScopePath,
  logger: Logger,
  allowRestrictedFeatures: Boolean,
  suppressWarningOptions: SuppressWarningOptions,
  maybeRecoverOnError: BuildException => Option[BuildException]
)(
  using ScalaCliInvokeData
) {
  def preprocess(content: String): Either[BuildException, PreprocessedDirectives] = for {
    directives <- ExtractedDirectives.from(content.toCharArray, path, logger, maybeRecoverOnError)
    res        <- preprocess(directives)
  } yield res

  def preprocess(extractedDirectives: ExtractedDirectives)
    : Either[BuildException, PreprocessedDirectives] = either {
    val ExtractedDirectives(directives, directivesPositions) = extractedDirectives

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
          applyDirectiveHandlers(directives, usingDirectiveHandlers)
        usingDirectivesWithRequirements: PartiallyProcessedDirectives[
          List[WithBuildRequirements[BuildOptions]]
        ] <-
          applyDirectiveHandlers(
            regularUsingDirectives.unused,
            usingDirectiveWithReqsHandlers
          )
        targetDirectives: PartiallyProcessedDirectives[BuildRequirements] <-
          applyDirectiveHandlers(
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

    val experimentalDirectivesUsed =
      buildOptionsWithTargetRequirements.experimentalUsed ++
        buildOptionsWithoutRequirements.experimentalUsed ++
        scopedBuildRequirements.experimentalUsed

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
              directivesPositions = directivesPositions,
              experimentalUsed = experimentalDirectivesUsed
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
    handlers: Seq[DirectiveHandler[T]]
  ): Either[BuildException, PartiallyProcessedDirectives[T]] = {
    val configMonoidInstance = implicitly[ConfigMonoid[T]]
    val shouldSuppressExperimentalFeatures =
      suppressWarningOptions.suppressExperimentalFeatureWarning.getOrElse(false)

    def handleValues(
      handler: DirectiveHandler[T],
      scopedDirective: ScopedDirective,
      logger: Logger
    ): Either[BuildException, ProcessedDirective[T]] =
      if !allowRestrictedFeatures && (handler.isRestricted || handler.isExperimental) then
        Left(DirectiveErrors(
          ::(WarningMessages.powerDirectiveUsedInSip(scopedDirective, handler), Nil),
          // TODO: use positions from ExtractedDirectives to get the full directive underlined
          DirectiveUtil.positions(scopedDirective.directive.values, path)
        ))
      else
        handler.handleValues(scopedDirective, logger)

    val handlersMap: Map[String, DirectiveHandler[T]] = (for {
      handler <- handlers
      key     <- handler.keys
    } yield key -> handler).toMap

    val (used, unused) = directives.partition(d => handlersMap.contains(d.key))

    val res = used.flatMap {
      case d @ StrictDirective(k, _) =>
        handlersMap.get(k).map { handler =>
          handleValues(handler, ScopedDirective(d, path, cwd), logger)
        }
    }.flatMap {
      case Left(e: BuildException) => maybeRecoverOnError(e).map(Left(_)).toSeq
      case r @ Right(_)            => Seq(r)
    }           // Seq[Either[BuildException, ProcessedDirective[T]]]
      .sequence // Either[BuildException, Seq[ProcessedDirective[T]]]
      .left.map(CompositeBuildException(_))
      .map { processedDirectives =>
        processedDirectives.foldLeft((configMonoidInstance.zero, Seq.empty[Scoped[T]])) {
          case ((globalAcc, scopedAcc), ProcessedDirective(global, scoped)) => (
              global.fold(globalAcc)(ns => configMonoidInstance.orElse(ns, globalAcc)),
              scopedAcc ++ scoped
            )
        }
      }

    res.map { case (g, s) =>
      val expDirs = used.filter { case d @ StrictDirective(k, _) =>
        handlersMap.get(k).exists(_.isExperimental)
      }
      PartiallyProcessedDirectives(g, s, unused, experimentalUsed = expDirs)
    }
  }
}
