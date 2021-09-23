package scala.build.preprocessing

import pureconfig.ConfigSource

import scala.build.config.ConfigFormat
import scala.build.errors.BuildException
import scala.build.Inputs

case object ConfigPreprocessor extends Preprocessor {
  def preprocess(input: Inputs.SingleElement)
    : Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case c: Inputs.ConfigFile if os.isFile(c.path) =>
        val source = ConfigSource.string(os.read(c.path))
        val conf = source.load[ConfigFormat] match {
          case Left(err) =>
            sys.error(s"Parsing ${c.path}:" + err.prettyPrint(indentLevel = 2))
          case Right(conf0) =>
            conf0
        }

        Some(Right(Seq(PreprocessedSource.NoSourceCode(Some(conf.buildOptions), None, c.path))))
      case _ =>
        None
    }
}
