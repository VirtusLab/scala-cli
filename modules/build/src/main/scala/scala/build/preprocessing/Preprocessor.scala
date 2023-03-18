package scala.build.preprocessing

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.input.{Inputs, SingleElement}
import scala.build.options.SuppressWarningOptions

trait Preprocessor {
  def preprocess(
    input: SingleElement,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e),
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  ): Option[Either[BuildException, Seq[PreprocessedSource]]]
}
