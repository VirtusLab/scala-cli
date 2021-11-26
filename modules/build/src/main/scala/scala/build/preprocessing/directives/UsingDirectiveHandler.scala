package scala.build.preprocessing.directives
import scala.build.errors.BuildException
import scala.build.options.BuildOptions
import scala.build.preprocessing.{ScopePath, Scoped}

trait UsingDirectiveHandler extends DirectiveHandler[BuildOptions] {
  // Loose / fastparse-based directives
  def handle(directive: Directive, cwd: ScopePath): Option[Either[BuildException, BuildOptions]]
  def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath
  ): Either[BuildException, (Option[BuildOptions], Seq[Scoped[BuildOptions]])] =
    if (keys.isEmpty)
      sys.error("Cannot happen")
    else
      throw new NotImplementedError(
        "using_directives-based directives need to override handleValues"
      )
}
