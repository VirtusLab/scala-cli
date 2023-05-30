package scala.build.preprocessing

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.input.{Inputs, ScalaCliInvokeData, SingleElement, VirtualData}
import scala.build.options.{
  BuildOptions,
  BuildRequirements,
  SuppressWarningOptions,
  WithBuildRequirements
}
import scala.build.preprocessing.directives.PreprocessedDirectives

case object DataPreprocessor extends Preprocessor {
  def preprocess(
    input: SingleElement,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e),
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  )(using ScalaCliInvokeData): Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case file: VirtualData =>
        val res = either {
          val content = new String(file.content, StandardCharsets.UTF_8)
          val preprocessedDirectives: PreprocessedDirectives = value {
            DirectivesPreprocessor.preprocess(
              content,
              Left(file.subPath.toString),
              file.scopePath,
              logger,
              allowRestrictedFeatures,
              suppressWarningOptions,
              maybeRecoverOnError
            )
          }
          val inMemory = Seq(
            PreprocessedSource.InMemory(
              originalPath = Left(file.source),
              relPath = file.subPath,
              code = content,
              wrapperParamsOpt = None,
              options = Some(preprocessedDirectives.globalUsings),
              optionsWithTargetRequirements = preprocessedDirectives.usingsWithReqs,
              requirements = Some(BuildRequirements()),
              scopedRequirements = Nil,
              mainClassOpt = None,
              scopePath = file.scopePath,
              preprocessedDirectives.directivesPositions
            )
          )
          inMemory
        }
        Some(res)
      case _ =>
        None
    }
}
