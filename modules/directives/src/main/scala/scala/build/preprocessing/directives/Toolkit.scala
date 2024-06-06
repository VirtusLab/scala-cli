package scala.build.preprocessing.directives

import coursier.core.Version
import dependency.*

import scala.build.Positioned
import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.internal.Constants
import scala.build.options.BuildRequirements.ScopeRequirement
import scala.build.options.WithBuildRequirements.*
import scala.build.options.*
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Toolkit")
@DirectiveExamples("//> using toolkit 0.1.0")
@DirectiveExamples("//> using toolkit default")
@DirectiveExamples("//> using test.toolkit default")
@DirectiveUsage(
  "//> using toolkit _version_",
  "`//> using toolkit` _version_"
)
@DirectiveDescription(
  s"Use a toolkit as dependency (not supported in Scala 2.12), 'default' version for Scala toolkit: ${Constants.toolkitDefaultVersion}, 'default' version for typelevel toolkit: ${Constants.typelevelToolkitDefaultVersion}"
)
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
  val typelevel = "typelevel"
  val scala     = "scala"

  def maxScalaNativeWarningMsg(
    toolkitName: String,
    toolkitVersion: String,
    maxNative: String
  ): String =
    s"$toolkitName $toolkitVersion does not support Scala Native ${Constants.scalaNativeVersion}, $maxNative should be used instead."

  object TypelevelToolkit {
    def unapply(s: Option[String]): Boolean =
      s.contains(typelevel) || s.contains(Constants.typelevelOrganization)
  }

  object ScalaToolkit {
    def unapply(s: Option[String]): Boolean =
      s.isEmpty || s.contains(Constants.toolkitOrganization) || s.contains(scala)
  }

  case class ToolkitDefinitions(
    isScalaToolkitDefault: Boolean = false,
    scalaToolkitExplicitVersion: Option[String] = None,
    isTypelevelToolkitDefault: Boolean = false,
    typelevelToolkitExplicitVersion: Option[String] = None
  )

  /** @param toolkitCoords
    *   the toolkit coordinates
    * @return
    *   the `toolkit` and `toolkit-test` dependencies with the appropriate build requirements
    */
  def resolveDependenciesWithRequirements(toolkitCoords: Positioned[String]): List[(
    WithBuildRequirements[Positioned[DependencyLike[NameAttributes, NameAttributes]]],
    ToolkitDefinitions
  )] =
    toolkitCoords match
      case Positioned(positions, coords) =>
        val tokens            = coords.split(':')
        val rawVersion        = tokens.last
        def isDefault         = rawVersion == "default"
        val notDefaultVersion = if rawVersion == "latest" then "latest.release" else rawVersion
        val flavor            = tokens.dropRight(1).headOption
        val (org, v, trv: ToolkitDefinitions) = flavor match {
          case TypelevelToolkit() =>
            val typelevelToolkitVersion =
              if isDefault then Constants.typelevelToolkitDefaultVersion
              else notDefaultVersion
            val explicitVersion =
              if isDefault then None else Some(typelevelToolkitVersion)
            (
              Constants.typelevelOrganization,
              typelevelToolkitVersion,
              ToolkitDefinitions(
                isTypelevelToolkitDefault = isDefault,
                typelevelToolkitExplicitVersion = explicitVersion
              )
            )
          case ScalaToolkit() | None =>
            val scalaToolkitVersion =
              if isDefault then Constants.toolkitDefaultVersion
              else notDefaultVersion
            val explicitVersion =
              if isDefault then None else Some(scalaToolkitVersion)
            (
              Constants.toolkitOrganization,
              scalaToolkitVersion,
              ToolkitDefinitions(
                isScalaToolkitDefault = isDefault,
                scalaToolkitExplicitVersion = explicitVersion
              )
            )
          case Some(org) => (org, notDefaultVersion, ToolkitDefinitions())
        }
        List(
          Positioned(positions, dep"$org::${Constants.toolkitName}::$v,toolkit")
            .withEmptyRequirements -> trv,
          Positioned(positions, dep"$org::${Constants.toolkitTestName}::$v,toolkit")
            .withScopeRequirement(Scope.Test) -> trv
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
    .map {
      case (
            WithBuildRequirements(requirements, positionedDep),
            ToolkitDefinitions(
              isScalaToolkitDefault,
              explicitScalaToolkitVersion,
              isTypelevelToolkitDefault,
              _
            )
          ) =>
        val scalaToolkitMaxNativeVersions =
          if isScalaToolkitDefault then
            List(Constants.toolkitMaxScalaNative -> maxScalaNativeWarningMsg(
              toolkitName = "Scala Toolkit",
              toolkitVersion = Constants.toolkitDefaultVersion,
              maxNative = Constants.toolkitMaxScalaNative
            ))
          else
            explicitScalaToolkitVersion.toList
              .map(Version(_))
              .filter(_ <= Version(Constants.toolkitVersionForNative04))
              .flatMap(v =>
                List(Constants.scalaNativeVersion04 -> maxScalaNativeWarningMsg(
                  toolkitName = "Scala Toolkit",
                  toolkitVersion = v.toString(),
                  Constants.scalaNativeVersion04
                ))
              )
        val typelevelToolkitMaxNativeVersions =
          if isTypelevelToolkitDefault then
            List(Constants.typelevelToolkitMaxScalaNative -> maxScalaNativeWarningMsg(
              toolkitName = "TypeLevel Toolkit",
              toolkitVersion = Constants.typelevelToolkitDefaultVersion,
              maxNative = Constants.typelevelToolkitMaxScalaNative
            ))
          else Nil
        val maxNativeVersions =
          (scalaToolkitMaxNativeVersions ++ typelevelToolkitMaxNativeVersions).distinct
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
              ),
              scalaNativeOptions = ScalaNativeOptions(
                maxDefaultNativeVersions = maxNativeVersions
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
