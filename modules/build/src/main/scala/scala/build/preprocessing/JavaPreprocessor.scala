package scala.build.preprocessing

import java.nio.charset.StandardCharsets

import scala.build.Inputs
import scala.build.errors.BuildException

case object JavaPreprocessor extends Preprocessor {
  def preprocess(input: Inputs.SingleElement)
    : Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case j: Inputs.JavaFile =>
        Some(Right(Seq(PreprocessedSource.OnDisk(j.path, None, None, Nil, None))))

      case v: Inputs.VirtualJavaFile =>
        val content = new String(v.content, StandardCharsets.UTF_8)
        val s = PreprocessedSource.InMemory(
          Left(v.source),
          v.subPath,
          content,
          0,
          None,
          None,
          Nil,
          None,
          v.scopePath
        )
        Some(Right(Seq(s)))

      case _ => None
    }
}
