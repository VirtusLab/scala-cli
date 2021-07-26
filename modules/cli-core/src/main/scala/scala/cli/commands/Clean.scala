package scala.cli.commands

import caseapp._

import scala.build.{Inputs, Os}

object Clean extends ScalaCommand[CleanOptions] {
  override def group = "Main"
  def run(options: CleanOptions, args: RemainingArgs): Unit = {

    val inputs = Inputs(args.all, Os.pwd, options.directories.directories, defaultInputs = Inputs.default(Os.pwd)) match {
      case Left(message) =>
        System.err.println(message)
        sys.exit(1)
      case Right(i) => i
    }

    val workDir = inputs.workspace / ".scala"

    val logger = options.logging.logger
    if (os.exists(workDir)) {
      logger.debug(s"Working directory: $workDir")
      if (os.isDir(workDir)) {
        logger.log(s"Removing $workDir")
        os.remove.all(workDir)
      } else
        logger.log(s"$workDir is not a directory, ignoring it.")
    }
  }
}
