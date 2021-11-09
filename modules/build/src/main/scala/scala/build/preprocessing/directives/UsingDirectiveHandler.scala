package scala.build.preprocessing.directives

import com.virtuslab.using_directives.custom.model.Value

import scala.build.errors.BuildException
import scala.build.options.BuildOptions
import scala.build.preprocessing.ScopePath

trait UsingDirectiveHandler extends DirectiveHandler {
  // Loose / fastparse-based directives
  def handle(directive: Directive, cwd: ScopePath): Option[Either[BuildException, BuildOptions]]
  def handleValues(
    values: Seq[Value[_]],
    path: Either[String, os.Path],
    cwd: ScopePath
  ): Either[BuildException, BuildOptions] =
    if (keys.isEmpty)
      sys.error("Cannot happen")
    else
      throw new NotImplementedError(
        "using_directives-based directives need to override handleValues"
      )
}
