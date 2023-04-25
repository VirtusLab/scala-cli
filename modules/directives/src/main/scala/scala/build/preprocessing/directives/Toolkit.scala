package scala.build.preprocessing.directives

import coursier.core.{Repository, Version}
import dependency.*

import scala.annotation.tailrec
import scala.build.EitherCps.{either, value}
import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.internal.Constants
import scala.build.options.{
  BuildOptions,
  ClassPathOptions,
  JavaOpt,
  Scope,
  ShadowingSeq,
  WithBuildRequirements
}
import scala.build.preprocessing.directives.Toolkit.buildOptions
import scala.build.{Artifacts, Logger, Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Toolkit")
@DirectiveExamples("//> using toolkit \"0.1.0\"")
@DirectiveExamples("//> using toolkit \"latest\"")
@DirectiveExamples("//> using test.toolkit \"latest\"")
@DirectiveUsage(
  "//> using toolkit _version_",
  "`//> using toolkit` _version_"
)
@DirectiveDescription("Use a toolkit as dependency")
@DirectiveLevel(SpecificationLevel.SHOULD)
final case class Toolkit(
  toolkit: Option[Positioned[String]] = None,
  @DirectiveName("test.toolkit")
  testToolkit: Option[Positioned[String]] = None
) extends HasBuildOptionsWithRequirements {
  def buildOptionsWithRequirements
    : Either[BuildException, List[WithBuildRequirements[BuildOptions]]] =
    Right {
      val mainBuildOpts = buildOptions(toolkit)
      val testBuildOpts = buildOptions(testToolkit)
      mainBuildOpts.toList.map(_.withEmptyRequirements) ++ testBuildOpts.toList.map(
        _.withScopeRequirement(Scope.Test)
      )
    }
}

object Toolkit {
  def resolveDependency(toolkitVersion: Positioned[String]) = toolkitVersion.map(version =>
    val v = if version == "latest" then "latest.release" else version
    dep"${Constants.toolkitOrganization}::${Constants.toolkitName}::$v,toolkit"
  )
  val handler: DirectiveHandler[Toolkit] = DirectiveHandler.derive

  def buildOptions(toolkit: Option[Positioned[String]]): Option[BuildOptions] = toolkit.map { t =>
    BuildOptions(
      classPathOptions = ClassPathOptions(
        extraDependencies = ShadowingSeq.from(List(resolveDependency(t)))
      )
    )
  }
}
