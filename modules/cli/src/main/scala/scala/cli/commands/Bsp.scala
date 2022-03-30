package scala.cli.commands

import caseapp._
import com.github.plokhotnyuk.jsoniter_scala.core._

import scala.build.{Build, Inputs}
import scala.build.bsp.BspThreads
import scala.build.options.BuildOptions
import scala.cli.CurrentParams
import scala.cli.commands.util.CommonOps._
import scala.cli.commands.util.SharedOptionsUtil._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Bsp extends ScalaCommand[BspOptions] {
  override def hidden = true
  def run(options: BspOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.shared.logging.verbosity
    if (options.shared.logging.verbosity >= 3)
      pprint.err.log(args)

    val sharedOptions: SharedOptions =
      options.jsonOptions.map { optionsPath =>
        val content = os.read.bytes(os.Path(optionsPath, os.pwd))
        readFromArray(content)(SharedOptions.jsonCodec)
      }.getOrElse(options.shared)

    val buildOptionsToUse = buildOptions(sharedOptions)
    val bloopRifleConfig  = sharedOptions.bloopRifleConfig()
    val logger            = sharedOptions.logging.logger

    val argsToInputs: Seq[String] => Either[String, Inputs] =
      argsSeq => options.shared.inputs(argsSeq, () => Inputs.default())
        .map{ i =>
          if (options.shared.logging.verbosity >= 3)
            pprint.err.log(i)
          Build.updateInputs(i, buildOptionsToUse)
        }
    val inputs = options.shared.inputsOrExit(argsToInputs(args.all))
    CurrentParams.workspaceOpt = Some(inputs.workspace)
    BspThreads.withThreads { threads =>
      val bsp = scala.build.bsp.Bsp.create(
        inputs,
        argsToInputs,
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
