package scala.cli.commands

import caseapp._

import scala.cli.CurrentParams
import scala.cli.commands.util.CommonOps._
import scala.cli.commands.util.VerbosityOptionsUtil._

object Uninstall extends ScalaCommand[UninstallOptions] {
  override def runCommand(options: UninstallOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.bloopExit.logging.verbosityOptions.verbosity
    val interactive =
      options.bloopExit.logging.verbosityOptions.interactiveInstance(forceEnable = true)
    val logger = options.bloopExit.logging.logger

    val binDirPath =
      options.binDirPath.getOrElse(scala.build.Directories.default().binRepoDir / "scala-cli")
    val destBinPath = binDirPath / options.binaryName
    val cacheDir    = scala.build.Directories.default().cacheDir

    if (
      !Update.isScalaCLIInstalledByInstallationScript() && (options.binDir.isEmpty || !options.force)
    ) {
      logger.error(
        "Scala CLI was not installed by the installation script, please use your package manager to uninstall scala-cli."
      )
      sys.exit(1)
    }
    if (!options.force) {
      val fallbackAction = () => {
        logger.error(s"To uninstall scala-cli pass -f or --force")
        sys.exit(1)
      }
      val msg = s"Do you want to uninstall scala-cli?"
      interactive.confirmOperation(msg).getOrElse(fallbackAction())
    }
    if (os.exists(destBinPath)) {
      // uninstall completions
      logger.debug("Uninstalling completions...")
      UninstallCompletions.run(
        UninstallCompletionsOptions(options.sharedUninstallCompletions, options.bloopExit.logging),
        args
      )
      // exit bloop server
      logger.debug("Stopping Bloop server...")
      BloopExit.run(options.bloopExit, args)
      // remove scala-cli launcher
      logger.debug("Removing scala-cli binary...")
      os.remove.all(binDirPath)
      // remove scala-cli caches
      logger.debug("Removing scala-cli cache directory...")
      if (!options.skipCache) os.remove.all(cacheDir)
      logger.message("Uninstalled successfully.")
    }
    else {
      logger.error(s"Could't find scala-cli binary at $destBinPath.")
      sys.exit(1)
    }
  }
}
