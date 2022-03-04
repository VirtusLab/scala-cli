package scala.cli.commands

import caseapp._

import scala.build.Logger
import scala.cli.CurrentParams
import scala.cli.commands.Version.{getCurrentVersion, isOutdated, newestScalaCliVersion}
import scala.cli.internal.ProcUtil
import scala.io.StdIn.readLine
import scala.util.{Failure, Properties, Success, Try}

object Update extends ScalaCommand[UpdateOptions] {

  private def updateScalaCli(options: UpdateOptions, newVersion: String) = {
    if (!options.force)
      if (coursier.paths.Util.useAnsiOutput()) {
        println(s"Do you want to update scala-cli to version $newVersion [Y/n]")
        val response = readLine()
        if (response.toLowerCase != "y") {
          System.err.println("Abort")
          sys.exit(1)
        }
      }
      else {
        System.err.println(s"To update scala-cli to $newVersion pass -f or --force")
        sys.exit(1)
      }

    val installationScript =
      ProcUtil.downloadFile("https://virtuslab.github.io/scala-cli-packages/scala-setup.sh")

    // format: off
    val res = os.proc(
      "bash", "-s", "--",
      "--version", newVersion,
      "--force",
      "--binary-name", options.binaryName,
      "--bin-dir", options.installDirPath,
    ).call(
      cwd = os.pwd,
      stdin = installationScript,
      stdout = os.Inherit,
      check = false,
      mergeErrIntoOut = true
    )
    // format: on
    val output = res.out.text().trim
    if (res.exitCode != 0) {
      System.err.println(s"Error during updating scala-cli: $output")
      sys.exit(1)
    }
  }

  lazy val updateInstructions: String =
    s"""Your Scala CLI version is outdated. The newest version is $newestScalaCliVersion
       |It is recommended that you update Scala CLI through the same tool or method you used for its initial installation for avoiding the creation of outdated duplicates.""".stripMargin

  def update(options: UpdateOptions, maybeScalaCliBinPath: Option[os.Path]): Unit = {

    val currentVersion = getCurrentVersion(maybeScalaCliBinPath)

    val outDated = isOutdated(maybeScalaCliBinPath)

    if (!options.isInternalRun)
      if (outDated)
        updateScalaCli(options, newestScalaCliVersion)
      else println("ScalaCLI is up-to-date")
    else if (outDated)
      println(
        s"""Your ScalaCLI $currentVersion is outdated, please update ScalaCLI to $newestScalaCliVersion
           |Run 'curl -sSLf https://virtuslab.github.io/scala-cli-packages/scala-setup.sh | sh' to update ScalaCLI.""".stripMargin
      )
  }

  def checkUpdate(options: UpdateOptions): Unit = {

    val scalaCliBinPath = options.installDirPath / options.binaryName

    val programName = argvOpt.flatMap(_.headOption).getOrElse {
      sys.error("update called in a non-standard way :|")
    }

    lazy val isScalaCliInPath = // if binDir is non empty, we do not expect scala-cli in PATH. But having it there is useful in tests
      CommandUtils.getAbsolutePathToScalaCli(programName).contains(
        options.installDirPath.toString()
      ) || options.binDir.isDefined

    if (!os.exists(scalaCliBinPath) || !isScalaCliInPath) {
      if (!options.isInternalRun) {
        System.err.println(
          "Scala CLI was not installed by the installation script, please use your package manager to update scala-cli."
        )
        sys.exit(1)
      }
    }
    else if (Properties.isWin) {
      if (!options.isInternalRun) {
        System.err.println("ScalaCLI update is not supported on Windows.")
        sys.exit(1)
      }
    }
    else if (options.binaryName == "scala-cli") update(options, None)
    else
      update(options, Some(scalaCliBinPath))
  }

  def run(options: UpdateOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.verbosity.verbosity
    checkUpdate(options)
  }

  def checkUpdateSafe(logger: Logger): Unit = {
    Try {
      val classesDir =
        this.getClass.getProtectionDomain.getCodeSource.getLocation.toURI.toString
      val binRepoDir = build.Directories.default().binRepoDir.toString()
      // log about update only if scala-cli was installed from installation script
      if (classesDir.contains(binRepoDir))
        checkUpdate(UpdateOptions(isInternalRun = true))
    } match {
      case Failure(ex) =>
        logger.debug(s"Ignoring error during checking update: $ex")
      case Success(_) => ()
    }
  }
}
