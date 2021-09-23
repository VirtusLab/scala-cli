package scala.build.preprocessing

import scala.build.errors.BuildException
import scala.build.Inputs

trait Preprocessor {
  def preprocess(input: Inputs.SingleElement)
    : Option[Either[BuildException, Seq[PreprocessedSource]]]
}
