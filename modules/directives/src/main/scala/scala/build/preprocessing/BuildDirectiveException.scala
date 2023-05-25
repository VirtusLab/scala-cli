package scala.build.preprocessing

import scala.build.errors.BuildException
import scala.build.preprocessing.directives.DirectiveBuildException
import scala.build.preprocessing.directives.DirectiveUtil.toScalaCli

final class BuildDirectiveException(
  val directiveException: scala.cli.directivehandler.DirectiveException
) extends BuildException(
      directiveException.toString,
      positions = directiveException.positions.map(_.toScalaCli),
      cause = directiveException
    )

object BuildDirectiveException {
  def apply(ex: scala.cli.directivehandler.DirectiveException): BuildException =
    ex match {
      case e: DirectiveBuildException => e.buildException
      case _                          => new BuildDirectiveException(ex)
    }
}
