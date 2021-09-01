package scala.cli.commands

import caseapp._

import java.io.File

import scala.build.{Build, Inputs, Os}

object Compile extends ScalaCommand[CompileOptions] {
  override def group                                  = "Main"
  override def sharedOptions(options: CompileOptions) = Some(options.shared)
  def run(options: CompileOptions, args: RemainingArgs): Unit = {

    val inputs = options.shared.inputsOrExit(args)

    def postBuild(build: Build): Unit =
      if (options.classPath)
        for (s <- build.successfulOpt) {
          val cp = s.fullClassPath.map(_.toAbsolutePath.toString).mkString(File.pathSeparator)
          println(cp)
        }

    val buildOptions     = options.buildOptions
    val bloopRifleConfig = options.shared.bloopRifleConfig()

    if (options.watch.watch) {
      val watcher = Build.watch(
        inputs,
        buildOptions,
        bloopRifleConfig,
        options.shared.logger,
        postAction = () => WatchUtil.printWatchMessage()
      ) { build =>
        postBuild(build)
      }
      try WatchUtil.waitForCtrlC()
      finally watcher.dispose()
    }
    else {
      val build = Build.build(inputs, buildOptions, bloopRifleConfig, options.shared.logger)
      postBuild(build)
    }
  }

}
