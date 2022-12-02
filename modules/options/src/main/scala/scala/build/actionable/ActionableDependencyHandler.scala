package scala.build.actionable

import coursier.Versions
import coursier.parse.RepositoryParser
import dependency._

import scala.build.EitherCps.{either, value}
import scala.build.Positioned
import scala.build.actionable.ActionableDiagnostic._
import scala.build.actionable.errors.ActionableHandlerError
import scala.build.errors.{BuildException, RepositoryFormatError}
import scala.build.internal.Util._
import scala.build.options.BuildOptions
import scala.build.options.ScalaVersionUtil.versionsWithTtl0
import scala.concurrent.duration.DurationInt

case object ActionableDependencyHandler
    extends ActionableHandler[ActionableDependencyUpdateDiagnostic] {
  type Setting = Positioned[AnyDependency]

  override def extractSettings(options: BuildOptions): Seq[Positioned[AnyDependency]] =
    options.classPathOptions.extraDependencies.toSeq

  override def actionableDiagnostic(
    setting: Positioned[AnyDependency],
    buildOptions: BuildOptions
  ): Either[BuildException, Option[ActionableDependencyUpdateDiagnostic]] = either {
    val dependency     = setting.value
    val currentVersion = dependency.version
    val latestVersion  = value(findLatestVersion(buildOptions, dependency))

    if (latestVersion != currentVersion) {
      val msg = s"${dependency.render} is outdated, update to $latestVersion"
      Some(ActionableDependencyUpdateDiagnostic(msg, setting.positions, dependency, latestVersion))
    }
    else
      None
  }

  private def findLatestVersion(
    buildOptions: BuildOptions,
    dependency: AnyDependency
  ): Either[BuildException, String] = either {
    val scalaParams  = value(buildOptions.scalaParams)
    val cache        = buildOptions.finalCache
    val csModule     = value(dependency.toCs(scalaParams)).module
    val repositories = value(buildOptions.finalRepositories)

    value {
      cache.versionsWithTtl0(csModule, repositories).versions.latest(
        coursier.core.Latest.Stable
      ).toRight {
        new ActionableHandlerError(s"No latest version found for ${dependency.render}")
      }
    }
  }

}
