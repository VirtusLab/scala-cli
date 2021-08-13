package scala.cli.commands

import caseapp._

import scala.build.Build
import scala.build.bsp.BspThreads
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Bsp extends ScalaCommand[BspOptions] {
  def run(options: BspOptions, args: RemainingArgs): Unit = {

    if (options.shared.logging.verbosity >= 3)
      pprint.better.log(args)

    val buildOptions = options.buildOptions
    val bloopRifleConfig = options.shared.bloopRifleConfig()
    val logger = options.shared.logging.logger

    val inputs = {
      val initialInputs = options.shared.inputsOrExit(args)
      if (options.shared.logging.verbosity >= 3)
        pprint.better.log(initialInputs)
      Build.updateInputs(initialInputs, buildOptions)
    }

    BspThreads.withThreads { threads =>
      val bsp = scala.build.bsp.Bsp.create(
        inputs,
        buildOptions,
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
      } finally {
        bsp.shutdown()
      }
    }
  }
}
