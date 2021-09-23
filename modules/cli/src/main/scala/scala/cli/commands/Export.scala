package scala.cli.commands

import caseapp._

import scala.build.{BloopBuildClient, Build, CrossSources, Inputs, Logger, Sources}
import scala.build.EitherAwait.{either, value}
import scala.build.errors.BuildException
import scala.build.internal.CustomCodeWrapper
import scala.build.options.BuildOptions
import scala.build.GeneratedSource

import scala.cli.export._

object Export extends ScalaCommand[ExportOptions] {

  private def prepareBuild(
    inputs: Inputs,
    buildOptions: BuildOptions,
    logger: Logger,
    verbosity: Int
  ): Either[BuildException, (Sources, BuildOptions)] = either {

    logger.log("Preparing build")

    val crossSources = value {
      CrossSources.forInputs(
        inputs,
        Sources.defaultPreprocessors(
          buildOptions.scriptOptions.codeWrapper.getOrElse(CustomCodeWrapper)
        )
      )
    }
    val sources = crossSources.sources(buildOptions)

    if (verbosity >= 3)
      pprint.better.log(sources)

    val options0 = buildOptions.orElse(sources.buildOptions)

    (sources, options0)
  }

  def sbtBuildTool     = Sbt("1.5.5")
  def defaultBuildTool = sbtBuildTool

  def run(options: ExportOptions, args: RemainingArgs): Unit = {

    val logger      = options.shared.logger
    val inputs      = options.shared.inputsOrExit(args)
    val baseOptions = options.buildOptions

    val (sources, options0) =
      prepareBuild(inputs, baseOptions, logger, options.shared.logging.verbosity)
        .orExit(logger)

    val buildTool =
      if (options.sbt.getOrElse(true))
        sbtBuildTool
      else
        defaultBuildTool

    val project = buildTool.export(options0, sources)

    val output = options.output.getOrElse("dest")
    val dest   = os.Path(output, os.pwd)
    if (os.exists(dest)) {
      System.err.println(s"Error: $output already exists.")
      sys.exit(1)
    }

    os.makeDir.all(dest)
    project.writeTo(dest)
  }
}
