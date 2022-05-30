package scala.build.actionable

import coursier.Versions
import coursier.core.{Module, ModuleName, Organization}
import dependency._

import scala.build.EitherCps.{either, value}
import scala.build.Positioned
import scala.build.actionable.errors.ActionableHandlerError
import scala.build.errors.BuildException
import scala.build.options.BuildOptions
import scala.concurrent.duration.DurationInt

case object ActionableDependencyHandler extends ActionableHandler[AnyDependency] {

  override def extractPositionedOptions(options: BuildOptions): Seq[Positioned[AnyDependency]] =
    options.classPathOptions.extraDependencies.toSeq

  override def createActionableDiagnostic(
    option: Positioned[AnyDependency],
    buildOptions: BuildOptions
  ): Either[BuildException, Option[ActionableDiagnostic]] = either {

    val baseDependency = option.value
    val currentVersion = baseDependency.version
    val scalaParams    = value(buildOptions.scalaParams)
    val dependency     = scalaParams.map(baseDependency.applyParams(_)).getOrElse(baseDependency)

    val organization = Organization(dependency.organization)
    val moduleName   = ModuleName(s"${dependency.name}")
    val csModule     = Module(organization, moduleName, Map.empty)
    val cache        = buildOptions.finalCache

    val res = cache.withTtl(0.seconds).logger.use {
      Versions(cache)
        .withModule(csModule)
        .result()
        .unsafeRun()(cache.ec)
    }

    val dependencyLatestVersion = value(res.versions.latest(coursier.core.Latest.Release).toRight(
      new ActionableHandlerError(s"Not found dependency versions for ${baseDependency.render}")
    ))

    if (dependencyLatestVersion != currentVersion) {
      val msg  = s"${baseDependency.render} is outdated, update to $dependencyLatestVersion"
      val from = baseDependency.render
      val to   = s"${baseDependency.module.render}:$dependencyLatestVersion"
      Some(ActionableDiagnostic(msg, from, to, option.positions))
    }
    else
      None
  }
}
