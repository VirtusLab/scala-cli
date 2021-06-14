package scala.cli.commands

import caseapp._

import java.io.File

import scala.build.{Build, Inputs, Os}

object Compile extends ScalaCommand[CompileOptions] {
  override def group = "Main"
  def run(options: CompileOptions, args: RemainingArgs): Unit = {

    val inputs = Inputs(args.all, Os.pwd, options.shared.directories.directories) match {
      case Left(message) =>
        System.err.println(message)
        sys.exit(1)
      case Right(i) => i
    }

    def postBuild(build: Build): Unit =
      if (options.classPath)
        for (s <- build.successfulOpt) {
          val cp = s.fullClassPath.map(_.toAbsolutePath.toString).mkString(File.pathSeparator)
          println(cp)
        }

    val buildOptions = options.buildOptions

    if (options.shared.watch) {
      val watcher = Build.watch(inputs, buildOptions, options.shared.logger, Os.pwd, postAction = () => WatchUtil.printWatchMessage()) { build =>
        postBuild(build)
      }
      try WatchUtil.waitForCtrlC()
      finally watcher.dispose()
    } else {
      val build = Build.build(inputs, buildOptions, options.shared.logger, Os.pwd)
      postBuild(build)
    }
  }

}
