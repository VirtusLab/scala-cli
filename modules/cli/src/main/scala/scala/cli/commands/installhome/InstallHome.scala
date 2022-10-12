package scala.cli.commands.installhome

import caseapp.*
import coursier.env.{EnvironmentUpdate, ProfileUpdater}

import scala.build.Logger
import scala.cli.CurrentParams
import scala.cli.commands.util.CommonOps.*
import scala.cli.commands.{
  CommandUtils,
  CustomWindowsEnvVarUpdater,
  InstallHomeOptions,
  ScalaCommand
}
import scala.io.StdIn.readLine
import scala.util.Properties

object InstallHome extends ScalaCommand[InstallHomeOptions] {
  override def hidden: Boolean         = true
  override def scalaSpecificationLevel = SpecificationLevel.IMPLEMENTATION

  private def logEqual(version: String, logger: Logger) = {
    logger.message(s"$fullRunnerName $version is already installed and up-to-date.")
    sys.exit(0)
  }

  private def logUpdate(
    env: Boolean,
    newVersion: String,
    oldVersion: String,
    logger: Logger
  ): Unit =
    if (!env) logger.message(
      s"""$baseRunnerName $oldVersion is already installed and out-of-date.
         |$baseRunnerName will be updated to version $newVersion
         |""".stripMargin
    )

  private def logDowngrade(
    env: Boolean,
    newVersion: String,
    oldVersion: String,
    logger: Logger
  ): Unit =
    if (!env && coursier.paths.Util.useAnsiOutput()) {
      logger.message(
        s"$baseRunnerName $oldVersion is already installed and up-to-date."
      )
      logger.error(
        s"Do you want to downgrade $baseRunnerName to version $newVersion [Y/n]"
      )
      val response = readLine()
      if (response != "Y") {
        logger.message("Abort")
        sys.exit(1)
      }
    }
    else {
      logger.error(
        s"Error: $baseRunnerName is already installed $oldVersion and up-to-date. Downgrade to $newVersion pass -f or --force."
      )
      sys.exit(1)
    }

  override def runCommand(
    options: InstallHomeOptions,
    args: RemainingArgs,
    logger: Logger
  ): Unit = {
    val binDirPath =
      options.binDirPath.getOrElse(
        scala.build.Directories.default().binRepoDir / baseRunnerName
      )
    val destBinPath = binDirPath / options.binaryName

    val newScalaCliBinPath = os.Path(options.scalaCliBinaryPath, os.pwd)

    val newVersion: String =
      os.proc(newScalaCliBinPath, "version", "--cli-version").call(cwd = os.pwd).out.trim()

    // Backward compatibility - previous versions not have the `--version` parameter
    val oldVersion: String =
      if (os.isFile(destBinPath)) {
        val res = os.proc(destBinPath, "version", "--cli-version").call(cwd = os.pwd, check = false)
        if (res.exitCode == 0)
          res.out.trim()
        else
          "0.0.0"
      }
      else
        "0.0.0"

    if (os.exists(binDirPath))
      if (options.force) () // skip logging
      else if (newVersion == oldVersion) logEqual(newVersion, logger)
      else if (CommandUtils.isOutOfDateVersion(newVersion, oldVersion))
        logUpdate(options.env, newVersion, oldVersion, logger)
      else logDowngrade(options.env, newVersion, oldVersion, logger)

    if (os.exists(destBinPath)) os.remove(destBinPath)

    os.copy(
      from = newScalaCliBinPath,
      to = destBinPath,
      createFolders = true
    )
    if (!Properties.isWin)
      os.perms.set(destBinPath, os.PermSet.fromString("rwxr-xr-x"))

    if (options.env)
      println(s"""export PATH="$binDirPath:$$PATH"""")
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

      println(s"Successfully installed $baseRunnerName $newVersion")

      if (didUpdate) {
        if (Properties.isLinux)
          println(
            s"""|Profile file(s) updated.
                |To run $baseRunnerName, log out and log back in, or run 'source ~/.profile'""".stripMargin
          )
        if (Properties.isMac)
          println(
            s"To run $baseRunnerName, open new terminal or run 'source ~/.profile'"
          )
      }

    }
  }
}
