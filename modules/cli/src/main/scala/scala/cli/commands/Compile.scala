package scala.cli.commands

import caseapp._

import java.io.File

import scala.build.Build

object Compile extends ScalaCommand[CompileOptions] {
  override def group                                  = "Main"
  override def sharedOptions(options: CompileOptions) = Some(options.shared)
  def run(options: CompileOptions, args: RemainingArgs): Unit = {

    val inputs = options.shared.inputsOrExit(args)

    val cross = options.cross.cross.getOrElse(false)
    if (options.classPath && cross) {
      System.err.println(s"Error: cannot specify both --class-path and --cross")
      sys.exit(1)
    }

    def postBuild(build: Build): Unit =
      if (options.classPath)
        for (s <- build.successfulOpt) {
          val cp = s.fullClassPath.map(_.toAbsolutePath.toString).mkString(File.pathSeparator)
          println(cp)
        }

    val buildOptions     = options.buildOptions
    val bloopRifleConfig = options.shared.bloopRifleConfig()

    val logger = options.shared.logger

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
          postBuild(builds.main)
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
      postBuild(builds.main)
    }
  }

}
