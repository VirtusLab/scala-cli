package scala.cli.commands

import caseapp.*

import scala.cli.CurrentParams
import scala.cli.commands.util.CommonOps.*

object Directories extends ScalaCommand[DirectoriesOptions] {
  override def hidden: Boolean = true
  override def isRestricted    = true
  override def loggingOptions(options: DirectoriesOptions): Option[LoggingOptions] =
    Some(options.logging)
  override def runCommand(options: DirectoriesOptions, args: RemainingArgs): Unit = {
    val logger = options.logging.logger
    if (args.all.nonEmpty) {
      logger.error("The directories command doesn't accept arguments.")
      sys.exit(1)
    }

    val directories = options.directories.directories

    println("Local repository: " + directories.localRepoDir)
    println("Completions: " + directories.completionsDir)
    println("Virtual projects: " + directories.virtualProjectsDir)
    println("BSP sockets: " + directories.bspSocketDir)
    println("Bloop daemon directory: " + directories.bloopDaemonDir)
    println("Secrets directory: " + directories.secretsDir)
  }
}
