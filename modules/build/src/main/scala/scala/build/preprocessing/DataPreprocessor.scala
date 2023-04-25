package scala.build.preprocessing

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.input.{Inputs, SingleElement, VirtualData}
import scala.build.options.{
  BuildOptions,
  BuildRequirements,
  SuppressWarningOptions,
  WithBuildRequirements
}
import scala.build.preprocessing.DirectivesProcessor.DirectivesProcessorOutput
import scala.build.preprocessing.PreprocessingUtil.optionsAndPositionsFromDirectives

case object DataPreprocessor extends Preprocessor {
  def preprocess(
    input: SingleElement,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e),
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  ): Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case file: VirtualData =>
        val res = either {
          val content = new String(file.content, StandardCharsets.UTF_8)
          val (
            updatedOptions: BuildOptions,
            optsWithReqs: List[WithBuildRequirements[BuildOptions]],
            directivesPositions: Option[DirectivesPositions]
          ) = value {
            optionsAndPositionsFromDirectives(
              content,
              file.scopePath,
              Left(file.subPath.toString),
              logger,
              maybeRecoverOnError,
              allowRestrictedFeatures,
              suppressWarningOptions
            )
          }

          val inMemory = Seq(
            PreprocessedSource.InMemory(
              originalPath = Left(file.source),
              relPath = file.subPath,
              code = content,
              ignoreLen = 0,
              options = Some(updatedOptions),
              optionsWithTargetRequirements = optsWithReqs,
              requirements = Some(BuildRequirements()),
              scopedRequirements = Nil,
              mainClassOpt = None,
              scopePath = file.scopePath,
              directivesPositions
            )
          )
          inMemory
        }
        Some(res)
      case _ =>
        None
    }
}
