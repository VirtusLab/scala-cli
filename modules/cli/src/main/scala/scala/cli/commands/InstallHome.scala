package scala.cli.commands

import caseapp._
import coursier.env.{EnvironmentUpdate, ProfileUpdater}

import java.io.File
import scala.util.Properties

object InstallHome extends ScalaCommand[InstallHomeOptions] {
  override def hidden = true
  override def names = List(
    List("install", "home")
  )
  def run(options: InstallHomeOptions, args: RemainingArgs): Unit = {

    if (Properties.isWin)
      System.err.println(s"Install home is not supported yet on windows")

    // copy scala-cli to binary directory
    val binDirPath      = scala.build.Directories.default().binRepoDir
    val scalaCliBinPath = binDirPath / "scala-cli"

    if (os.exists(scalaCliBinPath)) os.remove(scalaCliBinPath)
    os.makeDir.all(binDirPath)
    os.copy(os.Path(options.scalaCliBinaryPath, os.pwd), scalaCliBinPath)

    val update    = EnvironmentUpdate(Nil, Seq("PATH" -> scalaCliBinPath.toString()))
    val updater   = ProfileUpdater()
    val didUpdate = updater.applyUpdate(update)

    if (didUpdate) {
      println("Successfully installed scala-cli")
    }
    else {
      System.err.println(s"ScalaCli is already installed")
    }

  }
}
