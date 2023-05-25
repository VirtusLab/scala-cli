package scala.build.preprocessing

import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.input.{Inputs, ScalaCliInvokeData, SingleElement}
import scala.build.options.SuppressWarningOptions
import scala.cli.directivehandler.DirectiveException

trait Preprocessor {
  def preprocess(
    input: SingleElement,
    logger: Logger,
    maybeRecoverOnError: BuildException => Option[BuildException] = e => Some(e),
    maybeRecoverOnDirectiveError: DirectiveException => Option[DirectiveException] = e => Some(e),
    allowRestrictedFeatures: Boolean,
    suppressWarningOptions: SuppressWarningOptions
  )(using ScalaCliInvokeData): Option[Either[BuildException, Seq[PreprocessedSource]]]
}
