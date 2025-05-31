package scala.build.actionable
import coursier.core.{Latest, Version}
import dependency.*

import scala.build.EitherCps.{either, value}
import scala.build.actionable.ActionableDiagnostic.*
import scala.build.errors.{BuildException, Severity}
import scala.build.internal.Constants
import scala.build.internal.Util.*
import scala.build.options.BuildOptions
import scala.build.options.ScalaVersionUtil.versions
import scala.build.{Logger, Positioned}

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
    buildOptions: BuildOptions,
    loggerOpt: Option[Logger]
  ): Either[BuildException, Option[ActionableDependencyUpdateDiagnostic]] = either {
    val dependency       = setting.value
    val currentVersion   = dependency.version
    val latestVersionOpt = value(findLatestVersion(buildOptions, setting, loggerOpt))

    for {
      latestVersion <- latestVersionOpt
      if Version(latestVersion) > Version(currentVersion) &&
      !isLatestSyntaxVersion(currentVersion)
      // filtering out toolkit-test to prevent double-update-diagnostic
      if !(dependency.userParams.exists(_._1 == Constants.toolkitName) &&
      dependency.module.name == Constants.toolkitTestName)
    } yield
      if dependency.userParams.exists(_._1 == Constants.toolkitName)
      then
        val toolkitSuggestion =
          if dependency.module.organization == Constants.toolkitOrganization then latestVersion
          else if dependency.module.organization == Constants.typelevelOrganization then
            s"typelevel:$latestVersion"
          else s"${dependency.module.organization}:$latestVersion"
        ActionableDependencyUpdateDiagnostic(
          setting.positions,
          currentVersion,
          latestVersion,
          dependencyModuleName = Constants.toolkitName,
          suggestion = toolkitSuggestion
        )
      else
        ActionableDependencyUpdateDiagnostic(
          setting.positions,
          currentVersion,
          latestVersion,
          dependencyModuleName = dependency.module.name,
          suggestion = dependency.copy(version = latestVersion).render
        )
  }

  /** Versions like 'latest.*': 'latest.release', 'latest.integration', 'latest.stable'
    */
  private def isLatestSyntaxVersion(version: String): Boolean = Latest(version).nonEmpty
  private def findLatestVersion(
    buildOptions: BuildOptions,
    setting: Positioned[AnyDependency],
    loggerOpt: Option[Logger]
  ): Either[BuildException, Option[String]] = either {
    val dependency   = setting.value
    val scalaParams  = value(buildOptions.scalaParams)
    val cache        = buildOptions.finalCache
    val csModule     = value(dependency.toCs(scalaParams)).module
    val repositories = value(buildOptions.finalRepositories)

    val latestVersionOpt = cache.versions(csModule, repositories)
      .versions
      .latest(coursier.core.Latest.Stable)

    if (latestVersionOpt.isEmpty)
      loggerOpt.foreach(_.diagnostic(
        s"No latest version found for ${dependency.render}",
        Severity.Warning,
        setting.positions
      ))

    latestVersionOpt
  }
}
