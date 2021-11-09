package scala.build.preprocessing.directives

import scala.build.Position
import scala.build.errors.BuildException
import scala.build.options.BuildRequirements
import scala.build.preprocessing.ScopePath

trait RequireDirectiveHandler extends DirectiveHandler {
  def handle(
    directive: Directive,
    cwd: ScopePath
  ): Option[Either[BuildException, BuildRequirements]]

  def handleValues(
    values: Seq[Any],
    cwd: ScopePath,
    positionOpt: Option[Position]
  ): Either[BuildException, BuildRequirements] =
    if (keys.isEmpty)
      sys.error("Cannot happen")
    else
      throw new NotImplementedError(
        "using_directives-based directives need to override handleValues"
      )
}
