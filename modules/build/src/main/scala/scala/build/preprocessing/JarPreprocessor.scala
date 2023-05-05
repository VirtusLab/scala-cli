package scala.build.preprocessing

import java.nio.charset.StandardCharsets

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.input.{Inputs, JarFile, ScalaCliInvokeData, SingleElement}
import scala.build.options.{
  BuildOptions,
  BuildRequirements,
  ClassPathOptions,
  SuppressWarningOptions
}

case object JarPreprocessor extends Preprocessor {
  def preprocess(
    input: SingleElement,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e),
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  )(using ScalaCliInvokeData): Option[Either[BuildException, Seq[PreprocessedSource]]] =
    input match {
      case jar: JarFile => Some(either {
          val buildOptions = BuildOptions().copy(
            classPathOptions = ClassPathOptions(
              extraClassPath = Seq(jar.path)
            )
          )
          Seq(PreprocessedSource.OnDisk(
            path = jar.path,
            options = Some(buildOptions),
            optionsWithTargetRequirements = List.empty,
            requirements = Some(BuildRequirements()),
            scopedRequirements = Nil,
            mainClassOpt = None,
            directivesPositions = None
          ))
        })
      case _ =>
        None
    }
}
