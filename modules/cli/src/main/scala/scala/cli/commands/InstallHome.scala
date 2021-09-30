package scala.cli.commands

import caseapp._
import coursier.env.{EnvironmentUpdate, ProfileUpdater}

import scala.io.StdIn.readLine
import scala.util.Properties

object InstallHome extends ScalaCommand[InstallHomeOptions] {
  override def hidden: Boolean = true
  def run(options: InstallHomeOptions, args: RemainingArgs): Unit = {

    val binDirPath      = scala.build.Directories.default().binRepoDir
    val scalaCliBinPath = binDirPath / "scala-cli"

    if (os.exists(scalaCliBinPath))
      if (options.force) os.remove.all(scalaCliBinPath)
      else if (coursier.paths.Util.useAnsiOutput()) {
        println(
          "scala-cli already exists. Do you want to override it [Y/n]"
        )
        val replace = readLine()
        if (replace == "Y")
          os.remove.all(scalaCliBinPath)
        else {
          System.err.println("Abort")
          sys.exit(1)
        }
      }
      else {
        System.err.println(
          s"Error: scala-cli already exists. Pass -f or --force to force erasing it."
        )
        sys.exit(1)
      }

    os.copy(
      from = os.Path(options.scalaCliBinaryPath, os.pwd),
      to = scalaCliBinPath / "scala-cli",
      createFolders = true
    )
    if (!Properties.isWin)
      os.perms.set(scalaCliBinPath / "scala-cli", os.PermSet.fromString("rwxrwxr-x"))

    val update = EnvironmentUpdate(Nil, Seq("PATH" -> scalaCliBinPath.toString()))

    val didUpdate =
      if (Properties.isWin) {
        val updater = CustomWindowsEnvVarUpdater().withUseJni(Some(coursier.paths.Util.useJni()))
        updater.applyUpdate(update)
      }
      else {
        val updater = ProfileUpdater()
        updater.applyUpdate(update)
      }

    if (didUpdate)
      println("Successfully installed scala-cli")
    else
      System.err.println(s"scala-cli is already installed")

  }
}
