package scala.cli.commands

import caseapp._

object Directories extends ScalaCommand[DirectoriesOptions] {
  def run(options: DirectoriesOptions, args: RemainingArgs): Unit = {
    if (args.all.nonEmpty) {
      System.err.println("The directories command doesn't accept arguments.")
      sys.exit(1)
    }

    val directories = options.directories.directories

    println("Local repository: " + directories.localRepoDir)
    println("Completions: " + directories.completionsDir)
    println("Virtual projects: " + directories.virtualProjectsDir)
    println("BSP sockets: " + directories.bspSocketDir)
  }
}
