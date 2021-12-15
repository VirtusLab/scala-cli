package scala.build.preprocessing.directives
import scala.build.errors.BuildException
import scala.build.options.BuildOptions
import scala.build.preprocessing.ScopePath

trait UsingDirectiveHandler extends DirectiveHandler[BuildOptions] {
  type ProcessedUsingDirective = ProcessedDirective[BuildOptions]
  // Loose / fastparse-based directives
  def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath
  ): Either[BuildException, ProcessedUsingDirective] =
    throw new NotImplementedError(
      "using_directives-based directives need to override handleValues"
    )
}
