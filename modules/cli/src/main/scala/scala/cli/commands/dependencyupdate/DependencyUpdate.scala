package scala.cli.commands.dependencyupdate

import caseapp.*
import os.Path

import scala.build.actionable.ActionableDependencyHandler
import scala.build.actionable.ActionableDiagnostic.ActionableDependencyUpdateDiagnostic
import scala.build.internal.CustomCodeWrapper
import scala.build.options.{BuildOptions, Scope}
import scala.build.{CrossSources, Logger, Position, Sources}
import scala.cli.CurrentParams
import scala.cli.commands.util.SharedOptionsUtil.*
import scala.cli.commands.{DependencyUpdateOptions, ScalaCommand, SharedOptions}

object DependencyUpdate extends ScalaCommand[DependencyUpdateOptions] {
  override def group = "Main"
  override def sharedOptions(options: DependencyUpdateOptions): Option[SharedOptions] =
    Some(options.shared)
  override def runCommand(
    options: DependencyUpdateOptions,
    args: RemainingArgs,
    logger: Logger
  ): Unit = {
    val verbosity    = options.shared.logging.verbosity
    val buildOptions = buildOptionsOrExit(options)

    val inputs = options.shared.inputs(args.all).orExit(logger)

    val (crossSources, _) =
      CrossSources.forInputs(
        inputs,
        Sources.defaultPreprocessors(
          buildOptions.scriptOptions.codeWrapper.getOrElse(CustomCodeWrapper),
          buildOptions.archiveCache,
          buildOptions.internal.javaClassNameVersionOpt,
          () => buildOptions.javaHome().value.javaCommand
        ),
        logger
      ).orExit(logger)

    val scopedSources = crossSources.scopedSources(buildOptions).orExit(logger)

    def generateActionableUpdateDiagnostic(scope: Scope)
      : Seq[ActionableDependencyUpdateDiagnostic] = {
      val sources = scopedSources.sources(scope, crossSources.sharedOptions(buildOptions))

      if (verbosity >= 3)
        pprint.err.log(sources)

      val options = buildOptions.orElse(sources.buildOptions)
      ActionableDependencyHandler.createActionableDiagnostics(options).orExit(logger)
    }

    val actionableMainUpdateDiagnostics = generateActionableUpdateDiagnostic(Scope.Main)
    val actionableTestUpdateDiagnostics = generateActionableUpdateDiagnostic(Scope.Test)
    val actionableUpdateDiagnostics =
      (actionableMainUpdateDiagnostics ++ actionableTestUpdateDiagnostics).distinct

    if (options.all)
      updateDependencies(actionableUpdateDiagnostics, logger)
    else {
      println("Updates")
      actionableUpdateDiagnostics.foreach(update =>
        println(s"   * ${update.oldDependency.render} -> ${update.newVersion}")
      )
      println(s"""|To update all dependencies run:
                  |    $baseRunnerName dependency-update --all""".stripMargin)
    }
  }

  private def updateDependencies(
    actionableUpdateDiagnostics: Seq[ActionableDependencyUpdateDiagnostic],
    logger: Logger
  ): Unit = {
    val groupedByFileDiagnostics =
      actionableUpdateDiagnostics.flatMap {
        diagnostic =>
          diagnostic.positions.collect {
            case file: Position.File =>
              file.path -> (file, diagnostic)
          }
      }.groupMap(_._1)(_._2)

    groupedByFileDiagnostics.foreach {
      case (Right(file), diagnostics) =>
        val sortedByLine       = diagnostics.sortBy(_._1.startPos._1).reverse
        val appliedDiagnostics = updateDependencies(file, sortedByLine)
        os.write.over(file, appliedDiagnostics)
        diagnostics.foreach(diagnostic =>
          logger.message(s"Updated dependency to: ${diagnostic._2.suggestion}")
        )
      case (Left(file), diagnostics) =>
        diagnostics.foreach {
          diagnostic =>
            logger.message(
              s"Warning: $fullRunnerName can't update ${diagnostic._2.suggestion} in $file"
            )
        }
    }
  }

  private def updateDependencies(
    file: Path,
    diagnostics: Seq[(Position.File, ActionableDependencyUpdateDiagnostic)]
  ): String = {
    val fileContent   = os.read(file)
    val startIndicies = Position.Raw.lineStartIndices(fileContent)

    diagnostics.foldLeft(fileContent) {
      case (fileContent, (file, diagnostic)) =>
        val (line, column) = (file.startPos._1, file.startPos._2)
        val startIndex     = startIndicies(line) + column
        val endIndex       = startIndex + diagnostic.oldDependency.render.length()

        val newDependency = diagnostic.suggestion
        s"${fileContent.slice(0, startIndex)}$newDependency${fileContent.drop(endIndex)}"
    }
  }

}
