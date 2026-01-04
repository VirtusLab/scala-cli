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
      
      // Run dependency analysis if requested
      if options.withUnusedDeps || options.withExplicitDeps then
        runDependencyAnalysis(options, inputs, buildOpts, logger)
      
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
  
  private def runDependencyAnalysis(
    options: FixOptions,
    inputs: scala.build.input.Inputs,
    buildOpts: scala.build.options.BuildOptions,
    logger: Logger
  ): Unit = {
    logger.message("Analyzing project dependencies...")
    
    try {
      val (crossSources, _) = scala.build.CrossSources.forInputs(
        inputs,
        preprocessors = scala.build.Sources.defaultPreprocessors(
          buildOpts.archiveCache,
          buildOpts.internal.javaClassNameVersionOpt,
          () => buildOpts.javaHome().value.javaCommand
        ),
        logger = logger,
        suppressWarningOptions = buildOpts.suppressWarningOptions,
        exclude = buildOpts.internal.exclude,
        download = buildOpts.downloader
      ).orExit(logger)
      
      val sharedOptions = crossSources.sharedOptions(buildOpts)
      val scopedSources = crossSources.scopedSources(sharedOptions).orExit(logger)
      val sources = scopedSources.sources(
        scala.build.options.Scope.Main,
        sharedOptions,
        inputs.workspace,
        logger
      ).orExit(logger)
      
      val artifacts = buildOpts.artifacts(logger, scala.build.options.Scope.Main).orExit(logger)
      
      // Run the analysis
      val analysisResultEither = DependencyAnalyzer.analyzeDependencies(
        sources,
        buildOpts,
        artifacts,
        logger
      )
      
      if analysisResultEither.isRight then {
        val analysisResult = analysisResultEither.toOption.get
        
        // Report results
        if options.withUnusedDeps then {
          reportUnusedDependencies(analysisResult.unusedDependencies, logger)
        }
        
        if options.withExplicitDeps then {
          reportMissingDependencies(analysisResult.missingExplicitDependencies, logger)
        }
      } else {
        logger.error(s"Dependency analysis failed: ${analysisResultEither.left.getOrElse("Unknown error")}")
      }
    } catch {
      case ex: Exception =>
        logger.error(s"Error during dependency analysis: ${ex.getMessage}")
        logger.debug(ex.getStackTrace.map(_.toString).mkString("\n"))
    }
  }
  
  private def reportUnusedDependencies(
    unusedDeps: Seq[DependencyAnalyzer.UnusedDependency],
    logger: Logger
  ): Unit = {
    if unusedDeps.isEmpty then {
      logger.message("✓ No unused dependencies found.")
    } else {
      logger.message(s"\n⚠ Found ${unusedDeps.length} potentially unused dependencies:\n")
      unusedDeps.foreach { unused =>
        val dep = unused.dependency
        logger.message(s"  • ${dep.organization}:${dep.name}:${dep.version}")
        logger.message(s"    ${unused.reason}")
        logger.message(s"    Consider removing: //> using dep ${dep.render}\n")
      }
      logger.message("Note: This analysis is based on import statements and may produce false positives.")
      logger.message("Dependencies might be used via reflection, service loading, or other mechanisms.\n")
    }
  }
  
  private def reportMissingDependencies(
    missingDeps: Seq[DependencyAnalyzer.MissingDependency],
    logger: Logger
  ): Unit = {
    if missingDeps.isEmpty then {
      logger.message("✓ All directly used dependencies are explicitly declared.")
    } else {
      logger.message(s"\n⚠ Found ${missingDeps.length} transitive dependencies that are directly used:\n")
      missingDeps.foreach { missing =>
        logger.message(s"  • ${missing.organizationModule}:${missing.version}")
        logger.message(s"    ${missing.reason}")
        if missing.usedInFiles.nonEmpty then {
          logger.message(s"    Used in: ${missing.usedInFiles.map(_.last).mkString(", ")}")
        }
        logger.message(s"    Consider adding: //> using dep ${missing.organizationModule}:${missing.version}\n")
      }
      logger.message("Note: These dependencies are currently available transitively but should be declared explicitly.")
      logger.message("This ensures your build remains stable if upstream dependencies change.\n")
    }
  }
}
