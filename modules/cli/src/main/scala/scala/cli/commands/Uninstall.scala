package scala.cli.commands

import caseapp._

import scala.io.StdIn.readLine
import scala.cli.CurrentParams

object Uninstall extends ScalaCommand[UninstallOptions] {
  def run(options: UninstallOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.verbosity.verbosity
    val binDirPath =
      options.binDirPath.getOrElse(scala.build.Directories.default().binRepoDir / "scala-cli")
    val destBinPath = binDirPath / options.binaryName

    if (!options.force) {
      println("Do you want to uninstall scala-cli [Y/n]")
      val response = readLine()
      if (response != "Y") {
        System.err.println("Abort")
        sys.exit(1)
      }
    }
    if (os.exists(destBinPath)) {
      BloopExit.run(options.bloopExit, args)
      os.remove.all(binDirPath)
      println("Uninstalled sucessfully")
    } else if (!Update.isScalaCLIInstalledByInstallationScript()) {
      System.err.println("Scala CLI was not installed by the installation script, please use your package manager to uninstall scala-cli.")
      sys.exit(1)
    } else {
      System.err.println(s"Could't find $destBinPath.")
      sys.exit(1)
    } 
  }
}
