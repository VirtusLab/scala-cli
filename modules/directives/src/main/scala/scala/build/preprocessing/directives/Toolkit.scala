package scala.build.preprocessing.directives

import dependency.*

import scala.build.Positioned
import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.internal.Constants
import scala.build.options.{
  BuildOptions,
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
    toolkit.map(t => Toolkit.buildOptions(t).map(_.withEmptyRequirements)).toList ++
      testToolkit.map(tt => Toolkit.buildOptions(tt).map(_.withScopeRequirement(Scope.Test))).toList
}

object Toolkit {
  def resolveDependencies(toolkitCoords: Positioned[String])
    : List[Positioned[DependencyLike[NameAttributes, NameAttributes]]] =
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
        flavor
          .map(_ => List(Positioned(positions, dep"$org::${Constants.toolkitName}::$v,toolkit")))
          .getOrElse {
            List(
              Positioned(positions, dep"$org::${Constants.toolkitName}::$v,toolkit"),
              Positioned(positions, dep"$org::${Constants.toolkitTestName}::$v,toolkit")
            )
          }
  val handler: DirectiveHandler[Toolkit] = DirectiveHandler.derive
  def buildOptions(t: Positioned[String]): Either[BuildException, BuildOptions] =
    Right {
      BuildOptions(
        classPathOptions = ClassPathOptions(
          extraDependencies = ShadowingSeq.from(resolveDependencies(t))
        )
      )
    }
}
