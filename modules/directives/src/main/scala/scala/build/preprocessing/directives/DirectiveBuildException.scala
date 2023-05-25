package scala.build.preprocessing.directives

import scala.build.errors.BuildException
import scala.build.preprocessing.BuildDirectiveException
import scala.cli.directivehandler.DirectiveException

class DirectiveBuildException(val buildException: BuildException)
    extends DirectiveException(buildException.toString, cause = buildException)

object DirectiveBuildException {
  def apply(ex: BuildException): DirectiveException =
    ex match {
      case e: BuildDirectiveException => e.directiveException
      case _                          => new DirectiveBuildException(ex)
    }
}
