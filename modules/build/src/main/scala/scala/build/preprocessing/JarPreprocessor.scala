package scala.build.preprocessing

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.input.{Inputs, JarFile, SingleElement}
import scala.build.options.{
  BuildOptions,
  BuildRequirements,
  ClassPathOptions,
  SuppressWarningOptions
}
import scala.build.preprocessing.PreprocessingUtil.optionsAndPositionsFromDirectives

case object JarPreprocessor extends Preprocessor {
  def preprocess(
    input: SingleElement,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e),
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  ): Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case jar: JarFile => Some(either {
          val buildOptions = BuildOptions().copy(
            classPathOptions = ClassPathOptions(
              extraClassPath = Seq(jar.path)
            )
          )
          Seq(PreprocessedSource.OnDisk(
            jar.path,
            Some(buildOptions),
            Some(BuildRequirements()),
            Nil,
            None,
            None
          ))
        })
      case _ =>
        None
    }
}
