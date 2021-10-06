package scala.cli.commands

import caseapp._
import coursier.env.{EnvironmentUpdate, ProfileUpdater}

import scala.io.StdIn.readLine
import scala.util.{Properties, Try}

object InstallHome extends ScalaCommand[InstallHomeOptions] {
  override def hidden: Boolean = true

  private def isOutOfDate(newVersion: String, oldVersion: String): Boolean = {
    val versionOrdering = Ordering.by { (_: String).split("""\.""").map(_.toInt).toIterable }
    versionOrdering.gt(newVersion, oldVersion)
  }

  def run(options: InstallHomeOptions, args: RemainingArgs): Unit = {

    val binDirPath =
      options.binDirPath.getOrElse(scala.build.Directories.default().binRepoDir / "scala-cli")
    val newScalaCliBinPath = os.Path(options.scalaCliBinaryPath, os.pwd)

    val newVersion: String =
      os.proc(newScalaCliBinPath, "about", "--version").call(cwd = os.pwd).out.text.trim

    // Backward compatibility - previous versions not have the `--version` parameter
    val oldVersion: String = Try {
      os.proc(binDirPath / options.binaryName, "about", "--version").call(cwd =
        os.pwd).out.text.trim
    }.toOption.getOrElse("0.0.0")

    if (os.exists(binDirPath)) {
      if (options.force) os.remove.all(binDirPath)
      else if (newVersion == oldVersion) {
        System.err.println(
          s"Scala-cli $newVersion is already installed and up-to-date."
        )
        sys.exit(1)
      }
      else if (isOutOfDate(newVersion, oldVersion)) {
        if (!options.env) println(
          s"""scala-cli $oldVersion is already installed and out-of-date.
             |scala-cli will be updated to version $newVersion
             |""".stripMargin
        )
        os.remove.all(binDirPath)
      }
      else {
        if (!options.env && coursier.paths.Util.useAnsiOutput()) {
          println(s"scala-cli $oldVersion is already installed and up-to-date.")
          println(s"Do you want to downgrade scala-cli to version $newVersion [Y/n]")
          val response = readLine()
          if (response != "Y") {
            System.err.println("Abort")
            sys.exit(1)
          }
          else
            os.remove.all(binDirPath)
        }
        else {
          System.err.println(
            s"Error: scala-cli is already installed $oldVersion and up-to-date. Downgrade to $newVersion pass -f or --force."
          )
          sys.exit(1)
        }
      }

    os.copy(
      from = newScalaCliBinPath,
      to = binDirPath / options.binaryName,
      createFolders = true
    )
    if (!Properties.isWin)
      os.perms.set(binDirPath / options.binaryName, os.PermSet.fromString("rwxrwxr-x"))

    if (options.env) {
      println(s""" export PATH="$$PATH:$binDirPath" """)
    }
    else {

      val update = EnvironmentUpdate(Nil, Seq("PATH" -> binDirPath.toString()))

      val didUpdate =
        if (Properties.isWin) {
          val updater = CustomWindowsEnvVarUpdater().withUseJni(Some(coursier.paths.Util.useJni()))
          updater.applyUpdate(update)
        }
        else {
          val updater = ProfileUpdater()
          updater.applyUpdate(update)
        }

      if (didUpdate) "Profile was updated"

      println(s"Successfully installed scala-cli $newVersion")
    }
  }
}
