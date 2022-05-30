package scala.build.actionable

import coursier.Versions
import coursier.cache.FileCache
import coursier.core.{Module, ModuleName, Organization}
import dependency._

import scala.build.Positioned
import scala.build.options.BuildOptions
import scala.concurrent.duration.DurationInt

case object ActionableDependencyHandler extends ActionableHandler[AnyDependency] {

  val cache = FileCache()

  override def extractValues(options: BuildOptions): Seq[Positioned[AnyDependency]] =
    options.classPathOptions.extraDependencies.toSeq

  override def createActionableDiagnostic(
    value: Positioned[AnyDependency],
    options: BuildOptions
  ): ActionableDiagnostic = {

    val dependency         = value.value
    val scalaParams        = options.scalaParams.getOrElse(sys.error("sys.err")).get
    val scalaBinaryVersion = scalaParams.scalaBinaryVersion

    val organization = Organization(dependency.organization)
    val moduleName   = ModuleName(s"${dependency.name}_$scalaBinaryVersion")
    val csModule     = Module(organization, moduleName, Map.empty)

    val res = cache.withTtl(0.seconds).logger.use {
      Versions(cache)
        .withModule(csModule)
        .result()
        .unsafeRun()(cache.ec)
    }

    val dependencyLatestVersion = res.versions.latest(coursier.core.Latest.Release).get
    val msg  = s"${dependency.render} is outdated, please update to $dependencyLatestVersion"
    val from = dependency.render
    val to   = s"${dependency.module.render}:$dependencyLatestVersion"
    ActionableDiagnostic(
      msg,
      from,
      to,
      positions = value.positions
    )
  }
}
