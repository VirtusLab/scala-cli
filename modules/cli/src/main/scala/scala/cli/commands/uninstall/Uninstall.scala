package scala.cli.commands.uninstall

import caseapp.*

import scala.build.Logger
import scala.cli.CurrentParams
import scala.cli.commands.bloop.BloopExit
import scala.cli.commands.uninstallcompletions.UninstallCompletions
import scala.cli.commands.update.Update
import scala.cli.commands.util.CommonOps.*
import scala.cli.commands.util.VerbosityOptionsUtil.*
import scala.cli.commands.{ScalaCommand, UninstallCompletionsOptions, UninstallOptions}

object Uninstall extends ScalaCommand[UninstallOptions] {
  override def runCommand(options: UninstallOptions, args: RemainingArgs, logger: Logger): Unit = {
    val interactive =
      options.bloopExit.logging.verbosityOptions.interactiveInstance(forceEnable = true)

    val binDirPath =
      options.binDirPath.getOrElse(
        scala.build.Directories.default().binRepoDir / baseRunnerName
      )
    val destBinPath = binDirPath / options.binaryName
    val cacheDir    = scala.build.Directories.default().cacheDir

    if (
      !Update.isScalaCLIInstalledByInstallationScript && (options.binDir.isEmpty || !options.force)
    ) {
      logger.error(
        s"$fullRunnerName was not installed by the installation script, please use your package manager to uninstall $baseRunnerName."
      )
      sys.exit(1)
    }
    if (!options.force) {
      val fallbackAction = () => {
        logger.error(s"To uninstall $baseRunnerName pass -f or --force")
        sys.exit(1)
      }
      val msg = s"Do you want to uninstall $baseRunnerName?"
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
      logger.debug(s"Removing $baseRunnerName binary...")
      os.remove.all(binDirPath)
      // remove scala-cli caches
      logger.debug(s"Removing $baseRunnerName cache directory...")
      if (!options.skipCache) os.remove.all(cacheDir)
      logger.message("Uninstalled successfully.")
    }
    else {
      logger.error(s"Could't find $baseRunnerName binary at $destBinPath.")
      sys.exit(1)
    }
  }
}
