package scala.build.preprocessing.directives

import dependency.*

import scala.build.Positioned
import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.internal.Constants
import scala.build.options.BuildRequirements.ScopeRequirement
import scala.build.options.WithBuildRequirements.*
import scala.build.options.{
  BuildOptions,
  BuildRequirements,
  ClassPathOptions,
  Scope,
  ShadowingSeq,
  WithBuildRequirements
}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Toolkit")
@DirectiveExamples("//> using toolkit 0.1.0")
@DirectiveExamples("//> using toolkit latest")
@DirectiveExamples("//> using test.toolkit latest")
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
  def buildOptionsList: List[Either[BuildException, WithBuildRequirements[BuildOptions]]] =
    Toolkit.buildOptionsWithScopeRequirement(toolkit, defaultScope = None) ++
      Toolkit.buildOptionsWithScopeRequirement(testToolkit, defaultScope = Some(Scope.Test))
}

object Toolkit {
  def resolveDependenciesWithRequirements(toolkitCoords: Positioned[String])
    : List[WithBuildRequirements[Positioned[DependencyLike[NameAttributes, NameAttributes]]]] =
    toolkitCoords match
      case Positioned(positions, coords) =>
        val tokens  = coords.split(':')
        val version = tokens.last
        val v       = if version == "latest" then "latest.release" else version
        val flavor  = tokens.dropRight(1).headOption
        val org = flavor match {
          case Some("typelevel") => Constants.typelevelOrganization
          case Some(org)         => org
          case None              => Constants.toolkitOrganization
        }
        if org == Constants.toolkitOrganization then
          List(
            Positioned(positions, dep"$org::${Constants.toolkitName}::$v,toolkit")
              .withEmptyRequirements,
            Positioned(positions, dep"$org::${Constants.toolkitTestName}::$v,toolkit")
              .withScopeRequirement(Scope.Test)
          )
        else
          List(
            Positioned(positions, dep"$org::${Constants.toolkitName}::$v,toolkit")
              .withEmptyRequirements
          )
  val handler: DirectiveHandler[Toolkit] = DirectiveHandler.derive
  private def buildOptionsWithScopeRequirement(
    t: Option[Positioned[String]],
    defaultScope: Option[Scope]
  ): List[Either[BuildException, WithBuildRequirements[BuildOptions]]] = t
    .toList
    .flatMap(resolveDependenciesWithRequirements)
    .map { case WithBuildRequirements(requirements, positionedDep) =>
      positionedDep
        .withBuildRequirements {
          if requirements.scope.isEmpty then
            requirements.copy(scope = defaultScope.map(_.asScopeRequirement))
          else requirements
        }
        .map { dep =>
          BuildOptions(
            classPathOptions = ClassPathOptions(
              extraDependencies = ShadowingSeq.from(List(dep))
            )
          )
        }
    }
    .groupBy(_.requirements.scope.map(_.scope))
    .toList
    .map { (scope: Option[Scope], boWithReqsList: List[WithBuildRequirements[BuildOptions]]) =>
      Right {
        boWithReqsList.foldLeft {
          scope
            .map(s => BuildOptions.empty.withScopeRequirement(s))
            .getOrElse(BuildOptions.empty.withEmptyRequirements)
        } { (acc, boWithReqs) =>
          acc.map(_ orElse boWithReqs.value)
        }
      }
    }
}
