package scala.cli.commands

import caseapp._

import scala.cli.CurrentParams
import scala.cli.commands.util.VerbosityOptionsUtil._

object Uninstall extends ScalaCommand[UninstallOptions] {
  def run(options: UninstallOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.bloopExit.logging.verbosityOptions.verbosity
    val interactive =
      options.bloopExit.logging.verbosityOptions.interactiveInstance(forceEnable = true)

    val binDirPath =
      options.binDirPath.getOrElse(scala.build.Directories.default().binRepoDir / "scala-cli")
    val destBinPath = binDirPath / options.binaryName
    val cacheDir    = scala.build.Directories.default().cacheDir

    if (!options.force) {
      val fallbackAction = () => {
        System.err.println(s"To uninstall scala-cli pass -f or --force")
        sys.exit(1)
      }
      val msg = s"Do you want to uninstall scala-cli?"
      interactive.confirmOperation(msg).getOrElse(fallbackAction())
    }
    if (os.exists(destBinPath)) {
      // exit bloop server
      BloopExit.run(options.bloopExit, args)
      // remove scala-cli launcher
      os.remove.all(binDirPath)
      // remove scala-cli caches
      if (!options.skipCache) os.remove.all(cacheDir)
      println("Uninstalled sucessfully")
    }
    else if (!Update.isScalaCLIInstalledByInstallationScript()) {
      System.err.println(
        "Scala CLI was not installed by the installation script, please use your package manager to uninstall scala-cli."
      )
      sys.exit(1)
    }
    else {
      System.err.println(s"Could't find $destBinPath.")
      sys.exit(1)
    }
  }
}
