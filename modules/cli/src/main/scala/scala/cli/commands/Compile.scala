package scala.cli.commands

import caseapp._

import java.io.File

import scala.build.{Build, Inputs, Os}

object Compile extends ScalaCommand[CompileOptions] {
  override def group = "Main"
  override def sharedOptions(options: CompileOptions) = Some(options.shared)
  def run(options: CompileOptions, args: RemainingArgs): Unit =
    runCompile(options, args){ build =>
    if (options.classPath)
        for (s <- build.successfulOpt) {
          val cp = s.fullClassPath.map(_.toAbsolutePath.toString).mkString(File.pathSeparator)
          println(cp)
        }
      }

  def runCompile(options: CompileLikeOptions, args: RemainingArgs)(postBuild: Build => Unit): Unit = {
    val buildOptions = options.buildOptions
    val bloopRifleConfig = options.shared.bloopRifleConfig()

    val inputs = options.shared.inputsOrExit(args)
    if (options.watch.watch) {
      val watcher = Build.watch(inputs, buildOptions, bloopRifleConfig,
         options.shared.logger, options.shared.directories.directories, postAction = () => WatchUtil.printWatchMessage()) { build =>
        postBuild(build)
      }
      try WatchUtil.waitForCtrlC()
      finally watcher.dispose()
    } else {
      val build = Build.build(inputs, buildOptions, bloopRifleConfig, options.shared.logger, options.shared.directories.directories)
      postBuild(build)
    }
  }
}
