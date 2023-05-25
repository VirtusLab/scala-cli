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
import scala.build.preprocessing.directives.DirectiveUtil.given
import scala.cli.commands.SpecificationLevel
import scala.cli.directivehandler.*

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

  /** @param toolkitCoords
    *   the toolkit coordinates
    * @return
    *   the `toolkit` and `toolkit-test` dependencies with the appropriate build requirements
    */
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
        List(
          Positioned(positions, dep"$org::${Constants.toolkitName}::$v,toolkit")
            .withEmptyRequirements,
          Positioned(positions, dep"$org::${Constants.toolkitTestName}::$v,toolkit")
            .withScopeRequirement(Scope.Test)
        )
  val handler: DirectiveHandler[Toolkit] = DirectiveHandler.derive

  /** Returns the `toolkit` (and potentially `toolkit-test`) dependency with the appropriate
    * requirements.
    *
    * If [[defaultScope]] == None, it yields a List of up to 2 instances [[WithBuildRequirements]]
    * of [[BuildOptions]], one with the `toolkit` dependency and no requirements, and one with the
    * `toolkit-test` dependency and test scope requirements.
    *
    * If [[defaultScope]] == Some([[Scope.Test]]), then it yields a List with a single instance
    * containing both dependencies and the test scope requirement.
    *
    * @param t
    *   toolkit coordinates
    * @param defaultScope
    *   the scope requirement for the `toolkit` dependency
    * @return
    *   a list of [[Either]] [[BuildException]] [[WithBuildRequirements]] [[BuildOptions]]
    *   containing the `toolkit` and `toolkit-test` dependencies.
    */
  private def buildOptionsWithScopeRequirement(
    t: Option[Positioned[String]],
    defaultScope: Option[Scope]
  ): List[Either[BuildException, WithBuildRequirements[BuildOptions]]] = t
    .toList
    .flatMap(resolveDependenciesWithRequirements) // resolve dependencies
    .map { case WithBuildRequirements(requirements, positionedDep) =>
      positionedDep
        .withBuildRequirements {
          if requirements.scope.isEmpty then // if the scope is not set, set it to the default
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
        boWithReqsList.foldLeft { // merge all the BuildOptions with the same scope requirement
          scope
            .map(s => BuildOptions.empty.withScopeRequirement(s))
            .getOrElse(BuildOptions.empty.withEmptyRequirements)
        } { (acc, boWithReqs) =>
          acc.map(_ orElse boWithReqs.value)
        }
      }
    }
}
