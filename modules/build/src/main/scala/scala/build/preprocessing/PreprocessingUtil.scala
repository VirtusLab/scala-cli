package scala.build.preprocessing

import com.virtuslab.using_directives.custom.model.UsingDirectiveKind

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.{BuildException, FileNotFoundException}
import scala.build.options.{BuildOptions, SuppressWarningOptions, WithBuildRequirements}
import scala.build.preprocessing.DirectivesProcessor.DirectivesProcessorOutput
import scala.build.preprocessing.ExtractedDirectives.from
import scala.build.preprocessing.ScalaPreprocessor.*

object PreprocessingUtil {

  private def defaultCharSet = StandardCharsets.UTF_8

  def maybeRead(f: os.Path): Either[BuildException, String] =
    if (os.isFile(f)) Right(os.read(f, defaultCharSet))
    else Left(new FileNotFoundException(f))

  def optionsAndPositionsFromDirectives(
    content: String,
    scopePath: ScopePath,
    path: Either[String, os.Path],
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e),
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  ): Either[
    BuildException,
    (
      BuildOptions,
      List[WithBuildRequirements[BuildOptions]],
      Option[DirectivesPositions]
    )
  ] = either {
    val ExtractedDirectives(directives0, directivesPositions) =
      value(from(
        content.toCharArray,
        path,
        logger,
        scopePath,
        maybeRecoverOnError
      ))
    val updatedOptions: DirectivesProcessorOutput[BuildOptions] = value(DirectivesProcessor.process(
      directives0,
      usingDirectiveHandlers,
      path,
      scopePath,
      logger,
      allowRestrictedFeatures,
      suppressWarningOptions
    ))

    val directives1 = updatedOptions.unused
    val optionsWithReqs: DirectivesProcessorOutput[List[WithBuildRequirements[BuildOptions]]] =
      value(DirectivesProcessor.process(
        directives1,
        usingDirectiveWithReqsHandlers,
        path,
        scopePath,
        logger,
        allowRestrictedFeatures,
        suppressWarningOptions
      ))

    val (optionsWithActualRequirements, optionsWithEmptyRequirements) =
      optionsWithReqs.global.partition(_.requirements.nonEmpty)
    val summedOptionsWithNoRequirements =
      optionsWithEmptyRequirements
        .map(_.value)
        .foldLeft(updatedOptions.global)((acc, bo) => acc.orElse(bo))

    (summedOptionsWithNoRequirements, optionsWithActualRequirements, directivesPositions)
  }
}
