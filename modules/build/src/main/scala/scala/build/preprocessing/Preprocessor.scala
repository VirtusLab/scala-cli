package scala.build.preprocessing

import scala.build.Inputs

trait Preprocessor {
  def preprocess(input: Inputs.SingleElement): Option[Seq[PreprocessedSource]]
}
