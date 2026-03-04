package scala.cli.commands.fix

import caseapp.core.RemainingArgs

import scala.build.EitherCps.{either, value}
import scala.build.{BuildThreads, Logger}
import scala.cli.commands.ScalaCommand
import scala.cli.commands.shared.SharedOptions
import scala.cli.config.Keys
import scala.cli.util.ConfigDbUtils

object Fix extends ScalaCommand[FixOptions] {
  override def group                   = "Main"
  override def scalaSpecificationLevel = SpecificationLevel.EXPERIMENTAL
  override def sharedOptions(options: FixOptions): Option[SharedOptions] = Some(options.shared)

  override def runCommand(options: FixOptions, args: RemainingArgs, logger: Logger): Unit = {
    if options.areAnyRulesEnabled then {
      val inputs    = options.shared.inputs(args.all).orExit(logger)
      val buildOpts = buildOptionsOrExit(options)
      val configDb  = ConfigDbUtils.configDb.orExit(logger)
      if options.enableBuiltInRules then {
        logger.message("Running built-in rules...")
        if options.check then
          // TODO support --check for built-in rules: https://github.com/VirtusLab/scala-cli/issues/3423
          logger.message("Skipping, '--check' is not yet supported for built-in rules.")
        else {
          BuiltInRules.runRules(
            inputs = inputs,
            buildOptions = buildOpts,
            logger = logger
          )
          logger.message("Built-in rules completed.")
        }
      }
      if options.enableScalafix then
        either {
          logger.message("Running scalafix rules...")
          val threads                      = BuildThreads.create()
          val compilerMaker                = options.shared.compilerMaker(threads)
          val workspace: os.Path           = if args.all.isEmpty then os.pwd else inputs.workspace
          val actionableDiagnosticsEnabled = options.shared.logging.verbosityOptions.actions
            .orElse(configDb.get(Keys.actions).getOrElse(None))
          val scalafixExitCode: Int = value {
            ScalafixRules.runRules(
              buildOptions = buildOpts,
              scalafixOptions = options.scalafix,
              sharedOptions = options.shared,
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
