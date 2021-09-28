package scala.build.preprocessing

import scala.build.Inputs
import scala.build.errors.BuildException

trait Preprocessor {
  def preprocess(input: Inputs.SingleElement)
    : Option[Either[BuildException, Seq[PreprocessedSource]]]
}
