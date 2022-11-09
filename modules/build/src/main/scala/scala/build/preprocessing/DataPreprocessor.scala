package scala.build.preprocessing

import java.nio.charset.StandardCharsets

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.input.Inputs

case object DataPreprocessor extends Preprocessor {
  def preprocess(
    input: Inputs.SingleElement,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e),
    allowRestrictedFeatures: Boolean
  ): Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case file: Inputs.VirtualData =>
        val content = new String(file.content, StandardCharsets.UTF_8)

        val inMemory = Seq(
          PreprocessedSource.InMemory(
            Left(file.source),
            file.subPath,
            content,
            0,
            None,
            None,
            Nil,
            None,
            file.scopePath
          )
        )

        Some(Right(inMemory))
      case _ =>
        None
    }
}
