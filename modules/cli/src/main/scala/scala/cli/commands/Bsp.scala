package scala.cli.commands

import caseapp._
import upickle.default._

import scala.build.Build
import scala.build.bsp.BspThreads
import scala.build.options.BuildOptions
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Bsp extends ScalaCommand[BspOptions] {
  def run(options: BspOptions, args: RemainingArgs): Unit = {

    if (options.shared.logging.verbosity >= 3)
      pprint.stderr.log(args)

    val sharedOptions: SharedOptions =
      options.jsonOptions.map { optionsPath =>
        val source = os.read(os.Path(optionsPath, os.pwd))
        read[SharedOptions](source)
      }.getOrElse(options.shared)

    val buildOptionsToUse = buildOptions(sharedOptions)
    val bloopRifleConfig  = sharedOptions.bloopRifleConfig()
    val logger            = sharedOptions.logging.logger

    val inputs = {
      val initialInputs = options.shared.inputsOrExit(args)
      if (options.shared.logging.verbosity >= 3)
        pprint.stderr.log(initialInputs)
      Build.updateInputs(initialInputs, buildOptionsToUse)
    }

    BspThreads.withThreads { threads =>
      val bsp = scala.build.bsp.Bsp.create(
        inputs,
        buildOptionsToUse,
        logger,
        bloopRifleConfig,
        options.shared.logging.verbosity,
        threads,
        System.in,
        System.out
      )

      try {
        val doneFuture = bsp.run()
        Await.result(doneFuture, Duration.Inf)
      }
      finally bsp.shutdown()
    }
  }

  private def buildOptions(sharedOptions: SharedOptions): BuildOptions = {
    val baseOptions = sharedOptions.buildOptions(enableJmh = false, jmhVersion = None)
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
