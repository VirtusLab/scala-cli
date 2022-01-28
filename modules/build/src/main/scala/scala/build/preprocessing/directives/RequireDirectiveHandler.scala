package scala.build.preprocessing.directives
import scala.build.Logger
import scala.build.errors.BuildException
import scala.build.options.BuildRequirements
import scala.build.preprocessing.ScopePath

trait RequireDirectiveHandler extends DirectiveHandler[BuildRequirements] {
  type ProcessedRequireDirective = ProcessedDirective[BuildRequirements]

  override def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger
  ): Either[BuildException, ProcessedRequireDirective] =
    throw new NotImplementedError(
      "using_directives-based directives need to override handleValues"
    )
}
