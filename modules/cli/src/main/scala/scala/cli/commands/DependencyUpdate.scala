package scala.cli.commands

import caseapp._

import scala.build.actionable.{ActionableDependencyHandler, ActionableDiagnostic}
import scala.build.internal.CustomCodeWrapper
import scala.build.options.Scope
import scala.build.{CrossSources, Logger, Position, Sources}
import scala.cli.CurrentParams
import scala.cli.commands.util.SharedOptionsUtil._

object DependencyUpdate extends ScalaCommand[DependencyUpdateOptions] {
  override def group                                           = "Main"
  override def sharedOptions(options: DependencyUpdateOptions) = Some(options.shared)

  def run(options: DependencyUpdateOptions, args: RemainingArgs): Unit = {
    val verbosity = options.shared.logging.verbosity
    CurrentParams.verbosity = verbosity

    val inputs       = options.shared.inputsOrExit(args)
    val logger       = options.shared.logger
    val buildOptions = options.shared.buildOptions()

    val crossSources =
      CrossSources.forInputs(
        inputs,
        Sources.defaultPreprocessors(
          buildOptions.scriptOptions.codeWrapper.getOrElse(CustomCodeWrapper)
        ),
        logger
      ).orExit(logger)

    val scopedSources = crossSources.scopedSources(buildOptions).orExit(logger)
    val sources       = scopedSources.sources(Scope.Main, crossSources.sharedOptions(buildOptions))

    if (verbosity >= 3)
      pprint.err.log(sources)

    val options0 = buildOptions.orElse(sources.buildOptions)

    val dependencies = ActionableDependencyHandler.extractPositionedOptions(options0)
    val (errors, actionableUpdateDiagnostics) = dependencies.map(
      ActionableDependencyHandler.createActionableDiagnostic(_, options0)
    ).partitionMap(identity)

    errors.foreach(logger.debug(_))

    if (options.all)
      updateDependencies(actionableUpdateDiagnostics.flatten, logger)
  }

  private def updateDependencies(
    actionableUpdateDiagnostic: Seq[ActionableDiagnostic],
    logger: Logger
  ): Unit = {
    actionableUpdateDiagnostic.map { diagnostic =>
      diagnostic.positions.collect {
        case Position.File(Right(path), _, _) => path
      }.map { file =>
        logger.message(s"Updating dependency    ${diagnostic.from}")
        val content           = os.read(file)
        val appliedDiagnostic = content.replace(diagnostic.from, diagnostic.to)
        os.write.over(file, appliedDiagnostic)
        logger.message(s"Updated dependency to: ${diagnostic.to}")
      }
    }
  }

}
