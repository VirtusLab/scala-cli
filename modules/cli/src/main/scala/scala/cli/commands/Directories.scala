package scala.cli.commands

import caseapp._

import scala.cli.CurrentParams
import scala.cli.commands.util.CommonOps._

object Directories extends ScalaCommand[DirectoriesOptions] {
  override def hidden: Boolean = true
  override def isRestricted    = true

  def run(options: DirectoriesOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.verbosity.verbosity
    if (args.all.nonEmpty) {
      System.err.println("The directories command doesn't accept arguments.")
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
