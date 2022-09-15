package scala.cli.commands

import caseapp._

import java.io.File

import scala.build.options.Scope
import scala.build.{Build, BuildThreads, Builds, Os}
import scala.cli.CurrentParams
import scala.cli.commands.util.BuildCommandHelpers
import scala.cli.commands.util.CommonOps.SharedDirectoriesOptionsOps
import scala.cli.commands.util.SharedOptionsUtil._
import scala.cli.config.{ConfigDb, Keys}

object Compile extends ScalaCommand[CompileOptions] with BuildCommandHelpers {
  override def group                                                         = "Main"
  override def sharedOptions(options: CompileOptions): Option[SharedOptions] = Some(options.shared)

  def run(options: CompileOptions, args: RemainingArgs): Unit = {
    maybePrintGroupHelp(options)
    maybePrintSimpleScalacOutput(options, options.shared.buildOptions())
    val logger = options.shared.logger
    CurrentParams.verbosity = options.shared.logging.verbosity
    val inputs = options.shared.inputs(args.all).orExit(logger)
    CurrentParams.workspaceOpt = Some(inputs.workspace)
    SetupIde.runSafe(
      options.shared,
      inputs,
      logger,
      Some(name),
      args.all
    )
    if (CommandUtils.shouldCheckUpdate)
      Update.checkUpdateSafe(logger)

    val cross = options.cross.cross.getOrElse(false)
    if (options.printClassPath && cross) {
      System.err.println(s"Error: cannot specify both --print-class-path and --cross")
      sys.exit(1)
    }

    def postBuild(builds: Builds, allowExit: Boolean): Unit = {
      val failed = builds.all.exists {
        case _: Build.Failed => true
        case _               => false
      }
      val cancelled = builds.all.exists {
        case _: Build.Cancelled => true
        case _                  => false
      }
      if (failed) {
        System.err.println("Compilation failed")
        if (allowExit)
          sys.exit(1)
      }
      else if (cancelled) {
        System.err.println("Compilation cancelled")
        if (allowExit)
          sys.exit(1)
      }
      else {
        val successulBuildOpt =
          for {
            build <- builds.get(Scope.Test).orElse(builds.get(Scope.Main))
            s     <- build.successfulOpt
          } yield s
        if (options.printClassPath)
          for (s <- successulBuildOpt) {
            val cp = s.fullClassPath.map(_.toString).mkString(File.pathSeparator)
            println(cp)
          }
        successulBuildOpt.foreach(_.copyOutput(options.shared))
      }
    }

    val buildOptions = options.shared.buildOptions()
    val threads      = BuildThreads.create()

    val compilerMaker = options.shared.compilerMaker(threads)
    val configDb = ConfigDb.open(options.shared.directories.directories)
      .orExit(logger)
    val actionableDiagnostics =
      options.shared.logging.verbosityOptions.actions.orElse(
        configDb.get(Keys.actions).getOrElse(None)
      )

    if (options.watch.watchMode) {
      val watcher = Build.watch(
        inputs,
        buildOptions,
        compilerMaker,
        None,
        logger,
        crossBuilds = cross,
        buildTests = options.test,
        partial = None,
        actionableDiagnostics = actionableDiagnostics,
        postAction = () => WatchUtil.printWatchMessage()
      ) { res =>
        for (builds <- res.orReport(logger))
          postBuild(builds, allowExit = false)
      }
      try WatchUtil.waitForCtrlC()
      finally watcher.dispose()
    }
    else {
      val res = Build.build(
        inputs,
        buildOptions,
        compilerMaker,
        None,
        logger,
        crossBuilds = cross,
        buildTests = options.test,
        partial = None,
        actionableDiagnostics = actionableDiagnostics
      )
      val builds = res.orExit(logger)
      postBuild(builds, allowExit = true)
    }
  }

}
