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
          val inMemory = Seq(
            PreprocessedSource.InMemory(
              originalPath = Left(file.source),
              relPath = file.subPath,
              content = file.content,
              wrapperParamsOpt = None,
              options = None,
              optionsWithTargetRequirements = Nil,
              requirements = Some(BuildRequirements()),
              scopedRequirements = Nil,
              mainClassOpt = None,
              scopePath = file.scopePath,
              directivesPositions = None
            )
          )
          inMemory
        }
        Some(res)
      case _ =>
        None
    }
}
