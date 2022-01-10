package scala.build.preprocessing

import scala.build.errors.BuildException
import scala.build.{Inputs, Logger}

trait Preprocessor {
  def preprocess(
    input: Inputs.SingleElement,
    logger: Logger
  ): Option[Either[BuildException, Seq[PreprocessedSource]]]
}
