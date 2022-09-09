package scala.build.preprocessing

import scala.build.errors.BuildException
import scala.build.{Inputs, Logger}

trait Preprocessor {
  def preprocess(
    input: Inputs.SingleElement,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e),
    allowRestrictedFeatures: Boolean
  ): Option[Either[BuildException, Seq[PreprocessedSource]]]
}
