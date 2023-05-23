package scala.build.actionable

import coursier.Versions
import coursier.core.{Latest, Version}
import coursier.parse.RepositoryParser
import dependency.*

import scala.build.EitherCps.{either, value}
import scala.build.Positioned
import scala.build.actionable.ActionableDiagnostic.*
import scala.build.actionable.errors.ActionableHandlerError
import scala.build.errors.{BuildException, RepositoryFormatError}
import scala.build.internal.Constants
import scala.build.internal.Util.*
import scala.build.options.BuildOptions
import scala.build.options.ScalaVersionUtil.versions
import scala.concurrent.duration.DurationInt

case object ActionableDependencyHandler
    extends ActionableHandler[ActionableDependencyUpdateDiagnostic] {
  type Setting = Positioned[AnyDependency]

  override def extractSettings(options: BuildOptions): Seq[Positioned[AnyDependency]] =
    if (options.suppressWarningOptions.suppressOutdatedDependencyWarning.getOrElse(false))
      Nil
    else
      options.classPathOptions.extraDependencies.toSeq

  override def actionableDiagnostic(
    setting: Positioned[AnyDependency],
    buildOptions: BuildOptions
  ): Either[BuildException, Option[ActionableDependencyUpdateDiagnostic]] = either {
    val dependency     = setting.value
    val currentVersion = dependency.version
    val latestVersion  = value(findLatestVersion(buildOptions, dependency))
    if Version(latestVersion) > Version(currentVersion) && !isLatestSyntaxVersion(currentVersion)
    then
      if dependency.userParams.contains(Constants.toolkitName) &&
        dependency.module.name != Constants.toolkitTestName
      then
        val toolkitSuggestion =
          if dependency.module.organization == Constants.toolkitOrganization then latestVersion
          else if dependency.module.organization == Constants.typelevelOrganization then
            s"typelevel:$latestVersion"
          else s"${dependency.module.organization}:$latestVersion"
        Some(ActionableDependencyUpdateDiagnostic(
          setting.positions,
          currentVersion,
          latestVersion,
          dependencyModuleName = Constants.toolkitName,
          suggestion = toolkitSuggestion
        ))
      else if dependency.userParams.contains(Constants.toolkitName) &&
        dependency.module.name == Constants.toolkitTestName
      then None // filtering out toolkit-test to prevent double-update-diagnostic
      else
        Some(ActionableDependencyUpdateDiagnostic(
          setting.positions,
          currentVersion,
          latestVersion,
          dependencyModuleName = dependency.module.name,
          suggestion = dependency.copy(version = latestVersion).render
        ))
    else
      None
  }

  /** Versions like 'latest.*': 'latest.release', 'latest.integration', 'latest.stable'
    */
  private def isLatestSyntaxVersion(version: String): Boolean = Latest(version).nonEmpty
  private def findLatestVersion(
    buildOptions: BuildOptions,
    dependency: AnyDependency
  ): Either[BuildException, String] = either {
    val scalaParams  = value(buildOptions.scalaParams)
    val cache        = buildOptions.finalCache
    val csModule     = value(dependency.toCs(scalaParams)).module
    val repositories = value(buildOptions.finalRepositories)

    value {
      cache.versions(csModule, repositories).versions.latest(
        coursier.core.Latest.Stable
      ).toRight {
        new ActionableHandlerError(s"No latest version found for ${dependency.render}")
      }
    }
  }

}
