package scala.build.preprocessing

import java.nio.charset.StandardCharsets

import scala.build.Inputs

case object JavaPreprocessor extends Preprocessor {
  def preprocess(input: Inputs.SingleElement): Option[Seq[PreprocessedSource]] =
    input match {
      case j: Inputs.JavaFile =>
        Some(Seq(PreprocessedSource.OnDisk(j.path, None, None, None)))

      case v: Inputs.VirtualJavaFile =>
        val content = new String(v.content, StandardCharsets.UTF_8)
        val s = PreprocessedSource.InMemory(
          Left(v.source),
          v.subPath,
          content,
          0,
          None,
          None,
          None
        )
        Some(Seq(s))

      case _ => None
    }
}
