package scala.cli.commands

import caseapp._

import scala.build.actionable.ActionableDiagnostic.ActionableDependencyUpdateDiagnostic
import scala.build.actionable.ActionablePreprocessor
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

    def generateActionableUpdateDiagnostic(scope: Scope)
      : Seq[ActionableDependencyUpdateDiagnostic] = {
      val sources = scopedSources.sources(scope, crossSources.sharedOptions(buildOptions))

      if (verbosity >= 3)
        pprint.err.log(sources)

      val options = buildOptions.orElse(sources.buildOptions)
      val actionableDiagnostics =
        ActionablePreprocessor.generateActionableDiagnostics(options).orExit(logger)

      actionableDiagnostics.collect {
        case ad: ActionableDependencyUpdateDiagnostic => ad
      }
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
      println("""|To update all dependencies run: 
                 |    scala-cli dependency-update --all""".stripMargin)
    }
  }

  private def updateDependencies(
    actionableUpdateDiagnostics: Seq[ActionableDependencyUpdateDiagnostic],
    logger: Logger
  ): Unit = {
    actionableUpdateDiagnostics.foreach { diagnostic =>
      diagnostic.positions
        .collect { case file: Position.File => file }
        .foreach {
          case Position.File(Right(filePath), startPos, _) =>
            val lineIndex = startPos._1
            val appliedDiagnostic = splitFileContentByLine(filePath).map {
              case (line, index) if index == lineIndex + 1 =>
                updateDependency(line, startPos, diagnostic)
              case (line, _) => line
            }.mkString
            os.write.over(filePath, appliedDiagnostic)
            logger.message(s"Updated dependency to: ${diagnostic.to}")
          case Position.File(Left(file), _, _) =>
            logger.message(
              s"scala-cli can't update dependency to:${diagnostic.to} in virtual file: $file"
            )
        }
    }
  }

  private def splitFileContentByLine(file: os.Path) = {
    val content       = os.read(file)
    val startIndicies = Position.Raw.lineStartIndices(content).toList

    def lsplit(pos: List[Int], s: String): List[String] =
      pos match {
        case x :: rest => s.substring(0, x) :: lsplit(rest.map(_ - x), s.substring(x))
        case Nil       => List(s)
      }
    lsplit(startIndicies, content).zipWithIndex
  }

  private def updateDependency(
    line: String,
    pos: (Int, Int),
    diagnostic: ActionableDependencyUpdateDiagnostic
  ) = {
    val depColumnIndex = pos._2
    val (head, tail)   = line.splitAt(depColumnIndex)
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
