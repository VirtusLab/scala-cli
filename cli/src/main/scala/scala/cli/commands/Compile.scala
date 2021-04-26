package scala.cli.commands

import caseapp.core.app.CaseApp
import caseapp.core.RemainingArgs
import scala.cli.{Build, Inputs}

object Compile extends CaseApp[CompileOptions] {
  def run(options: CompileOptions, args: RemainingArgs): Unit = {

    val inputs = Inputs(args.all) match {
      case Left(message) =>
        System.err.println(message)
        sys.exit(1)
      case Right(i) => i
    }

    if (options.shared.watch) {
      val watcher = Build.watch(inputs, options.shared.buildOptions, options.shared.logger, postAction = () => WatchUtil.printWatchMessage()) { build =>
        ()
      }
      try WatchUtil.waitForCtrlC()
      finally watcher.dispose()
    } else {
      Build.build(inputs, options.shared.buildOptions, options.shared.logger)
    }
  }

}
