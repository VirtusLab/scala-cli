package scala.cli.commands

import caseapp._

import scala.build.actionable.ActionableDependencyHandler
import scala.build.actionable.ActionableDiagnostic.ActionableDependencyUpdateDiagnostic
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
          buildOptions.scriptOptions.codeWrapper.getOrElse(CustomCodeWrapper),
          buildOptions.archiveCache,
          buildOptions.internal.javaClassNameVersionOpt
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
    actionableUpdateDiagnostic: Seq[ActionableDependencyUpdateDiagnostic],
    logger: Logger
  ): Unit = {
    for {
      diagnostic                                  <- actionableUpdateDiagnostic
      Position.File(Right(filePath), startPos, _) <- diagnostic.positions
    } {
      val lineIndex = startPos._1
      val appliedDiagnostic = os.read.lines(filePath).zipWithIndex.map {
        case (line, index) if index == lineIndex =>
          val updatedDependency = updateDependency(line, startPos, diagnostic)
          updatedDependency
        case (line, _) => line
      }.mkString(System.lineSeparator())
      os.write.over(filePath, appliedDiagnostic)
      logger.message(s"Updated dependency to: ${diagnostic.to}")
    }
  }

  private def updateDependency(
    line: String,
    pos: (Int, Int),
    diagnostic: ActionableDependencyUpdateDiagnostic
  ) = {
    val depColumnIndex = pos._2
    val head           = line.take(depColumnIndex)
    val tail           = line.drop(depColumnIndex)
    if (tail.startsWith("$ivy.`")) {
      val last = tail.stripPrefix("$ivy.`").dropWhile(_ != '`')
      s"$head$$ivy.`${diagnostic.to}$last"
    }
    else if (tail.startsWith("$dep.`")) {
      val last = tail.stripPrefix("$dep.`").dropWhile(_ != '`')
      s"$head$$dep.`${diagnostic.to}$last"
    }
    else {
      val last = line.drop(depColumnIndex).dropWhile(_ != '\"')
      s"$head${diagnostic.to}$last"
    }
  }

}
