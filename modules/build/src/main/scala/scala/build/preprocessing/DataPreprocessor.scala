package scala.build.preprocessing

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.input.{Inputs, SingleElement, VirtualData}
import scala.build.options.BuildRequirements
import scala.build.preprocessing.PreprocessingUtil.optionsAndPositionsFromDirectives

case object DataPreprocessor extends Preprocessor {
  def preprocess(
    input: SingleElement,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e),
    allowRestrictedFeatures: Boolean
  ): Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case file: VirtualData =>
        val res = either {
          val content = new String(file.content, StandardCharsets.UTF_8)
          val (updatedOptions, directivesPositions) = value {
            optionsAndPositionsFromDirectives(
              content,
              file.scopePath,
              Left(file.subPath.toString),
              logger,
              maybeRecoverOnError,
              allowRestrictedFeatures
            )
          }

          val inMemory = Seq(
            PreprocessedSource.InMemory(
              Left(file.source),
              file.subPath,
              content,
              0,
              Some(updatedOptions.global),
              Some(BuildRequirements()),
              Nil,
              None,
              file.scopePath,
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
