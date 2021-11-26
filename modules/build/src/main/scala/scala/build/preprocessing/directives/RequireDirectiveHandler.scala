package scala.build.preprocessing.directives
import scala.build.errors.BuildException
import scala.build.options.BuildRequirements
import scala.build.preprocessing.{ScopePath, Scoped}

trait RequireDirectiveHandler extends DirectiveHandler[BuildRequirements] {
  def handle(
    directive: Directive,
    cwd: ScopePath
  ): Option[Either[BuildException, BuildRequirements]]

  def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath
  ): Either[BuildException, (Option[BuildRequirements], Seq[Scoped[BuildRequirements]])] =
    if (keys.isEmpty)
      sys.error("Cannot happen")
    else
      throw new NotImplementedError(
        "using_directives-based directives need to override handleValues"
      )
}
