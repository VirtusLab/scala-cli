package scala.build.preprocessing

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.input.{Inputs, SingleElement}

trait Preprocessor {
  def preprocess(
    input: SingleElement,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e),
    allowRestrictedFeatures: Boolean
  ): Option[Either[BuildException, Seq[PreprocessedSource]]]
}
