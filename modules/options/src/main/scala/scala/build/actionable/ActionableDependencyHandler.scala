package scala.build.actionable
import coursier.Versions
import coursier.cache.FileCache
import coursier.core.{Repository, Versions as CoreVersions}
import coursier.util.Task
import coursier.version.{Latest, Version}
import dependency.*

import scala.build.EitherCps.*
import scala.build.actionable.ActionableDiagnostic.*
import scala.build.errors.{BuildException, Severity}
import scala.build.internal.Constants
import scala.build.internal.Util.*
import scala.build.options.BuildOptions
import scala.build.{Logger, Positioned}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future, TimeoutException}
import scala.util.control.NonFatal

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
          if dependency.isScalaLangOrganization then latestVersion
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

  private val perRepoVersionsTimeout: FiniteDuration = 5.seconds

  private def mergeCoreVersions(parts: Seq[CoreVersions]): CoreVersions =
    val mergedAvailable = parts.flatMap(_.available0).distinctBy(_.asString).toList
    CoreVersions.empty.withAvailable0(mergedAvailable)

  private def findLatestVersion(
    buildOptions: BuildOptions,
    setting: Positioned[AnyDependency],
    loggerOpt: Option[Logger]
  ): Either[BuildException, Option[String]] = either {
    val dependency: AnyDependency            = setting.value
    val scalaParams: Option[ScalaParameters] = value(buildOptions.scalaParams)
    val cache: FileCache[Task]               = buildOptions.finalCache
    val csModule: coursier.core.Module       = value(dependency.toCs(scalaParams)).module
    val includeUserExtraRepositories         = !dependency.isScalaLangOrganization
    val repositories: Seq[Repository]        =
      value(buildOptions.finalRepositories(includeUserExtraRepositories))

    given ExecutionContext = cache.ec

    val perRepoFutures: Seq[Future[CoreVersions]] = repositories.map { repo =>
      val label                         = repo.toString.take(200)
      val listing: Future[CoreVersions] =
        Versions(cache)
          .withModule(csModule)
          .addRepositories(repo)
          .result()
          .future()
          .map(_.versions)
      listing.recover {
        case NonFatal(e) =>
          loggerOpt.foreach(_.debug(
            s"Failed listing versions for ${dependency.render} from repository $label: ${e.getMessage}"
          ))
          CoreVersions.empty
      }
    }

    val perRepoParts: Seq[CoreVersions] = perRepoFutures.map { f =>
      try Await.result(f, perRepoVersionsTimeout)
      catch {
        case _: TimeoutException =>
          loggerOpt.foreach(_.debug(
            s"Timeout listing versions for ${dependency.render} (after $perRepoVersionsTimeout)"
          ))
          CoreVersions.empty
        case NonFatal(e) =>
          loggerOpt.foreach(_.debug(
            s"Failed listing versions for ${dependency.render}: ${e.getMessage}"
          ))
          CoreVersions.empty
      }
    }

    val mergedVersions   = mergeCoreVersions(perRepoParts)
    val latestVersionOpt = mergedVersions.latest(Latest.Stable)

    if latestVersionOpt.isEmpty then
      loggerOpt.foreach(_.diagnostic(
        s"No latest version found for ${dependency.render}",
        Severity.Warning,
        setting.positions
      ))

    latestVersionOpt.map(_.asString)
  }
}
