package scala.cli.commands.directories

import caseapp.*

import scala.build.Logger
import scala.cli.CurrentParams
import scala.cli.commands.util.CommonOps.*
import scala.cli.commands.{DirectoriesOptions, ScalaCommand}

object Directories extends ScalaCommand[DirectoriesOptions] {
  override def hidden: Boolean = true
  
  override def scalaSpecificationLevel = SpecificationLevel.RESTRICTED

  override def runCommand(
    options: DirectoriesOptions,
    args: RemainingArgs,
    logger: Logger
  ): Unit = {
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
