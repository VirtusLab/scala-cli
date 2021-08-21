package scala.build.preprocessing

import scala.build.Inputs
import pureconfig.ConfigSource
import scala.build.config.ConfigFormat

case object ConfigPreprocessor extends Preprocessor {
  def preprocess(input: Inputs.SingleElement): Option[Seq[PreprocessedSource]] =
    input match {
      case c: Inputs.ConfigFile if os.isFile(c.path) =>
        val source = ConfigSource.string(os.read(c.path))
        val conf = source.load[ConfigFormat] match {
          case Left(err) =>
            sys.error(s"Parsing ${c.path}:" + err.prettyPrint(indentLevel = 2))
          case Right(conf0) =>
            conf0
        }

        Some(Seq(PreprocessedSource.NoSource(Some(conf.buildOptions))))
      case _ =>
        None
    }
}
