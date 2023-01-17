package scala.build.preprocessing.directives

import coursier.core.{Repository, Version}
import dependency.*

import scala.annotation.tailrec
import scala.build.EitherCps.{either, value}
import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.internal.Constants
import scala.build.options.{BuildOptions, ClassPathOptions, JavaOpt, Scope, ShadowingSeq}
import scala.build.{Artifacts, Logger, Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Toolkit")
@DirectiveExamples("//> using toolkit \"0.1.0\"")
@DirectiveExamples("//> using toolkit \"latest\"")
@DirectiveUsage(
  "//> using toolkit _version_",
  "`//> using toolkit` _version_"
)
@DirectiveDescription("Use a toolkit as dependency")
@DirectiveLevel(SpecificationLevel.SHOULD)
// format: off
final case class Toolkit(
  toolkit: Option[Positioned[String]] = None
) extends HasBuildOptions {
  // format: on
  def buildOptions: Either[BuildException, BuildOptions] = {
    val toolkitDep =
      toolkit.toList.map(Toolkit.resolveDependency)
    val buildOpt = BuildOptions(
      classPathOptions = ClassPathOptions(
        extraDependencies = ShadowingSeq.from(toolkitDep)
      )
    )
    Right(buildOpt)
  }
}

object Toolkit {
  def resolveDependency(toolkitVersion: Positioned[String]) = toolkitVersion.map(version =>
    val v = if version == "latest" then "latest.release" else version
    dep"${Constants.toolkitOrganization}::${Constants.toolkitName}:$v,toolkit"
  )
  val handler: DirectiveHandler[Toolkit] = DirectiveHandler.derive
}
