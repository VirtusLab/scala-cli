package scala.cli.commands

import caseapp.core.app.CaseApp
import caseapp.core.RemainingArgs

import java.io.File

import scala.cli.{Build, Inputs}

object Compile extends CaseApp[CompileOptions] {
  def run(options: CompileOptions, args: RemainingArgs): Unit = {

    val inputs = Inputs(args.all, os.pwd) match {
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

    if (options.shared.watch) {
      val watcher = Build.watch(inputs, options.shared.buildOptions, options.shared.logger, os.pwd, postAction = () => WatchUtil.printWatchMessage()) { build =>
        postBuild(build)
      }
      try WatchUtil.waitForCtrlC()
      finally watcher.dispose()
    } else {
      val build = Build.build(inputs, options.shared.buildOptions, options.shared.logger, os.pwd)
      postBuild(build)
    }
  }

}
