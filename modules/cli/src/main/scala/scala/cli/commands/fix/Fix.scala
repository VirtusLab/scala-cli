package scala.cli.commands.fix

import caseapp.core.RemainingArgs

import scala.build.EitherCps.{either, value}
import scala.build.Ops.EitherMap2
import scala.build.errors.{BuildException, CompositeBuildException}
import scala.build.input.*
import scala.build.internal.Constants
import scala.build.options.{BuildOptions, Scope, SuppressWarningOptions}
import scala.build.preprocessing.directives.*
import scala.build.preprocessing.{ExtractedDirectives, SheBang}
import scala.build.{BuildThreads, CrossSources, Logger, Position, Sources}
import scala.cli.commands.shared.SharedOptions
import scala.cli.commands.{ScalaCommand, SpecificationLevel}
import scala.cli.config.Keys
import scala.cli.util.ConfigDbUtils
import scala.collection.immutable.HashMap
import scala.util.chaining.scalaUtilChainingOps

object Fix extends ScalaCommand[FixOptions] {
  override def group                   = "Main"
  override def scalaSpecificationLevel = SpecificationLevel.EXPERIMENTAL
  override def sharedOptions(options: FixOptions): Option[SharedOptions] = Some(options.shared)

  override def runCommand(options: FixOptions, args: RemainingArgs, logger: Logger): Unit = {
    if options.areAnyRulesEnabled then {
      val inputs   = options.shared.inputs(args.all).orExit(logger)
      val configDb = ConfigDbUtils.configDb.orExit(logger)
      if options.enableBuiltInRules then {
        logger.message("Running built-in rules...")
        BuiltInRules.runRules(
          inputs = inputs,
          logger = logger
        )
        logger.message("Built-in rules completed.")
      }
      if options.enableScalafix then
        either {
          logger.message("Running scalafix rules...")
          val threads            = BuildThreads.create()
          val compilerMaker      = options.shared.compilerMaker(threads)
          val workspace: os.Path = if args.all.isEmpty then os.pwd else inputs.workspace
          val actionableDiagnosticsEnabled = options.shared.logging.verbosityOptions.actions
            .orElse(configDb.get(Keys.actions).getOrElse(None))
          val scalafixExitCode: Int = value {
            ScalafixRules.runRules(
              buildOptions = buildOptionsOrExit(options),
              scalafixOptions = options.scalafix,
              inputs = inputs,
              check = options.check,
              compilerMaker = compilerMaker,
              actionableDiagnostics = actionableDiagnosticsEnabled,
              workspace = workspace,
              logger = logger
            )
          }
          if scalafixExitCode != 1 then logger.message("scalafix rules completed.")
          else logger.error("scalafix rules failed.")
          sys.exit(scalafixExitCode)
        }
    }
    else logger.message("No rules were enabled. Did you disable everything intentionally?")
  }
}
