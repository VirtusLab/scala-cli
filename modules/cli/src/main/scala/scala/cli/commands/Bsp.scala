package scala.cli.commands

import caseapp._
import com.github.plokhotnyuk.jsoniter_scala.core._

import scala.build.EitherCps.{either, value}
import scala.build.bsp.{BspReloadableOptions, BspThreads}
import scala.build.errors.BuildException
import scala.build.options.BuildOptions
import scala.build.{Build, Inputs}
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

    val getSharedOptions: () => SharedOptions = () =>
      options.jsonOptions.map { optionsPath =>
        val content = os.read.bytes(os.Path(optionsPath, os.pwd))
        readFromArray(content)(SharedOptions.jsonCodec)
      }.getOrElse(options.shared)

    val argsToInputs: Seq[String] => Either[BuildException, Inputs] =
      argsSeq =>
        either {
          val sharedOptions = getSharedOptions()
          val initialInputs = value(sharedOptions.inputs(argsSeq, () => Inputs.default()))

          if (sharedOptions.logging.verbosity >= 3)
            pprint.err.log(initialInputs)

          Build.updateInputs(initialInputs, buildOptions(sharedOptions))
        }

    val bspReloadableOptionsReference = BspReloadableOptions.Reference { () =>
      val sharedOptions = getSharedOptions()
      BspReloadableOptions(
        buildOptions = buildOptions(sharedOptions),
        bloopRifleConfig = sharedOptions.bloopRifleConfig(),
        logger = sharedOptions.logging.logger,
        verbosity = sharedOptions.logging.verbosity
      )
    }

    val logger = getSharedOptions().logging.logger
    val inputs = argsToInputs(args.all).orExit(logger)
    CurrentParams.workspaceOpt = Some(inputs.workspace)
    BspThreads.withThreads { threads =>
      val bsp = scala.build.bsp.Bsp.create(
        argsToInputs,
        bspReloadableOptionsReference,
        threads,
        System.in,
        System.out
      )

      try {
        val doneFuture = bsp.run(inputs)
        Await.result(doneFuture, Duration.Inf)
      }
      finally bsp.shutdown()
    }
  }

  private def buildOptions(sharedOptions: SharedOptions): BuildOptions = {
    val baseOptions = sharedOptions.buildOptions()
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
