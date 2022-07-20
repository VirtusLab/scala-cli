package scala.cli.commands

import caseapp._

import java.io.File

import scala.build.options.Scope
import scala.build.{Build, BuildThreads, Builds, Os}
import scala.cli.CurrentParams
import scala.cli.commands.util.SharedOptionsUtil._
import scala.cli.config.{ConfigDb, Keys}
import scala.cli.commands.util.CommonOps.SharedDirectoriesOptionsOps

object Compile extends ScalaCommand[CompileOptions] {
  override def group                                                         = "Main"
  override def sharedOptions(options: CompileOptions): Option[SharedOptions] = Some(options.shared)

  def outputPath(options: CompileOptions): Option[os.Path] =
    options.output.filter(_.nonEmpty).map(p => os.Path(p, Os.pwd))

  def run(options: CompileOptions, args: RemainingArgs): Unit = {
    maybePrintGroupHelp(options)
    maybePrintSimpleScalacOutput(options, options.shared.buildOptions())
    CurrentParams.verbosity = options.shared.logging.verbosity
    val inputs = options.shared.inputsOrExit(args)
    CurrentParams.workspaceOpt = Some(inputs.workspace)
    val logger = options.shared.logger
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
    if (options.classPath && cross) {
      System.err.println(s"Error: cannot specify both --class-path and --cross")
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
        if (options.classPath)
          for (s <- successulBuildOpt) {
            val cp = s.fullClassPath.map(_.toString).mkString(File.pathSeparator)
            println(cp)
          }
        for (output <- outputPath(options); s <- successulBuildOpt)
          os.copy.over(s.output, output)
      }
    }

    val buildOptions = options.shared.buildOptions()
    val threads      = BuildThreads.create()

    val compilerMaker = options.shared.compilerMaker(threads)
    val configDb = ConfigDb.open(options.shared.directories.directories)
      .orExit(logger)
    val actionableDiagnostics = configDb.get(Keys.actionableDiagnostics).getOrElse(None)

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
