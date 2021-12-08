package scala.cli.commands

import caseapp._

import java.io.File

import scala.build.{Build, Builds}

object Compile extends ScalaCommand[CompileOptions] {
  override def group                                  = "Main"
  override def sharedOptions(options: CompileOptions) = Some(options.shared)
  def run(options: CompileOptions, args: RemainingArgs): Unit = {
    val inputs = options.shared.inputsOrExit(args)
    val logger = options.shared.logger
    SetupIde.runSafe(
      options.shared,
      inputs,
      logger,
      Some(name)
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
      else if (options.classPath)
        for (s <- builds.main.successfulOpt) {
          val cp = s.fullClassPath.map(_.toAbsolutePath.toString).mkString(File.pathSeparator)
          println(cp)
        }
    }

    val buildOptions     = options.buildOptions
    val bloopRifleConfig = options.shared.bloopRifleConfig()

    if (options.watch.watch) {
      val watcher = Build.watch(
        inputs,
        buildOptions,
        bloopRifleConfig,
        logger,
        crossBuilds = cross,
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
        bloopRifleConfig,
        logger,
        crossBuilds = cross
      )
      val builds = res.orExit(logger)
      postBuild(builds, allowExit = true)
    }
  }

}
