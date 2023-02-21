package scala.cli.commands.compile

import caseapp.*
import caseapp.core.help.HelpFormat

import java.io.File

import scala.build.options.{BuildOptions, Scope}
import scala.build.{Build, BuildThreads, Builds, Logger, Os}
import scala.cli.CurrentParams
import scala.cli.commands.publish.ConfigUtil.*
import scala.cli.commands.setupide.SetupIde
import scala.cli.commands.shared.SharedOptions
import scala.cli.commands.update.Update
import scala.cli.commands.util.BuildCommandHelpers
import scala.cli.commands.{CommandUtils, ScalaCommand, WatchUtil}
import scala.cli.config.{ConfigDb, Keys}
import scala.cli.util.ArgHelpers.*

object Compile extends ScalaCommand[CompileOptions] with BuildCommandHelpers {
  override def group = "Main"

  override def sharedOptions(options: CompileOptions): Option[SharedOptions] = Some(options.shared)

  override def scalaSpecificationLevel = SpecificationLevel.MUST
  val primaryHelpGroups: Seq[String] =
    Seq("Compilation", "Scala", "Java", "Watch", "Compilation server")

  override def helpFormat: HelpFormat = super.helpFormat.withPrimaryGroups(primaryHelpGroups)

  override def runCommand(options: CompileOptions, args: RemainingArgs, logger: Logger): Unit = {
    val buildOptions = buildOptionsOrExit(options)
    val inputs       = options.shared.inputs(args.all).orExit(logger)
    CurrentParams.workspaceOpt = Some(inputs.workspace)
    SetupIde.runSafe(
      options.shared,
      inputs,
      logger,
      buildOptions,
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

    val threads = BuildThreads.create()

    val compilerMaker = options.shared.compilerMaker(threads).orExit(logger)
    val configDb      = options.shared.configDb.orExit(logger)
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
      try WatchUtil.waitForCtrlC(() => watcher.schedule())
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
