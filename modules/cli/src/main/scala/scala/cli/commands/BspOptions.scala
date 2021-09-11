package scala.cli.commands

import caseapp._
import scala.build.options.BuildOptions

// format: off
final case class BspOptions(
  // FIXME There might be too many options in SharedOptions for the bsp command…
  @Recurse
    shared: SharedOptions = SharedOptions()
) {
  // format: on

  def buildOptions: BuildOptions = {
    val baseOptions = shared.buildOptions(enableJmh = false, jmhVersion = None)
    baseOptions.copy(
      classPathOptions = baseOptions.classPathOptions.copy(
        fetchSources = baseOptions.classPathOptions.fetchSources.orElse(Some(true))
      ),
      scalaOptions = baseOptions.scalaOptions.copy(
        generateSemanticDbs = baseOptions.scalaOptions.generateSemanticDbs.orElse(Some(true))
      ),
      internalDependencies = baseOptions.internalDependencies.copy(
        addRunnerDependencyOpt =
          baseOptions.internalDependencies.addRunnerDependencyOpt.orElse(Some(false))
      )
    )
  }
}

object BspOptions {
  implicit val parser = Parser[BspOptions]
  implicit val help   = Help[BspOptions]
}
