package scala.cli.commands.fix

import caseapp.core.RemainingArgs

import scala.build.EitherCps.{either, value}
import scala.build.internal.util.WarningMessages
import scala.build.internals.ConsoleUtils.ScalaCliConsole.warnPrefix
import scala.build.{BuildThreads, Logger}
import scala.cli.commands.ScalaCommand
import scala.cli.commands.shared.SharedOptions
import scala.cli.config.Keys
import scala.cli.util.ConfigDbUtils

object Fix extends ScalaCommand[FixOptions]:
  override def group                   = "Main"
  override def scalaSpecificationLevel = SpecificationLevel.EXPERIMENTAL
  override def sharedOptions(options: FixOptions): Option[SharedOptions] = Some(options.shared)

  override def runCommand(options: FixOptions, args: RemainingArgs, logger: Logger): Unit =
    if options.areAnyRulesEnabled then
      val inputs    = options.shared.inputs(args.all).orExit(logger)
      val buildOpts = buildOptionsOrExit(options)
      val configDb  = ConfigDbUtils.configDb.orExit(logger)
      if (buildOpts.notForBloopOptions.sloth || buildOpts.notForBloopOptions.slothAgent) &&
        !options.enableScalafix
      then
        logger.message(
          s"$warnPrefix ${WarningMessages.slothNotApplicable("the fix command without scalafix rules enabled")}"
        )
      val builtInRulesExitCode: Int =
        if options.enableBuiltInRules then
          logger.message("Running built-in rules...")
          val changesNeeded = BuiltInRules.runRules(
            inputs = inputs,
            buildOptions = buildOpts,
            check = options.check,
            logger = logger
          )
          if changesNeeded then
            logger.error("built-in rules failed.")
            1
          else
            logger.message("Built-in rules completed.")
            0
        else 0
      if options.enableScalafix then
        val scalafixResult = either:
          logger.message("Running scalafix rules...")
          val threads                      = BuildThreads.create()
          val compilerMaker                = options.shared.compilerMaker(threads)
          val workspace: os.Path           = if args.all.isEmpty then os.pwd else inputs.workspace
          val actionableDiagnosticsEnabled = options.shared.logging.verbosityOptions.actions
            .orElse(configDb.get(Keys.actions).getOrElse(None))
          val scalafixExitCode: Int = value:
            ScalafixRules.runRules(
              buildOptions = buildOpts,
              scalafixOptions = options.scalafix,
              sharedOptions = options.shared,
              inputs = inputs,
              check = options.check,
              // exec'ing scalafix would discard the built-in rules' failure
              allowExecve = builtInRulesExitCode == 0,
              compilerMaker = compilerMaker,
              actionableDiagnostics = actionableDiagnosticsEnabled,
              workspace = workspace,
              logger = logger
            )
          if scalafixExitCode != 1 then logger.message("scalafix rules completed.")
          else logger.error("scalafix rules failed.")
          sys.exit(math.max(scalafixExitCode, builtInRulesExitCode))
        scalafixResult.orExit(logger)
      else if builtInRulesExitCode != 0 then sys.exit(builtInRulesExitCode)
    else logger.message("No rules were enabled. Did you disable everything intentionally?")
