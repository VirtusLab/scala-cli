package scala.cli.commands

import caseapp._

import scala.build.Logger
import scala.io.StdIn.readLine
import scala.util.{Failure, Properties, Success, Try}

object Update extends ScalaCommand[UpdateOptions] {

  private def updateScalaCli(options: UpdateOptions, updateToVersion: String) = {
    if (coursier.paths.Util.useAnsiOutput()) {
      println(s"Do you want to update scala-cli to version $updateToVersion [Y/n]")
      val response = readLine()
      if (response != "Y") {
        System.err.println("Abort")
        sys.exit(1)
      }
    }
    else if (!options.force) {
      System.err.println(s"To update scala-cli to $updateToVersion pass -f or --force")
      sys.exit(1)
    }

    val installScript =
      os.proc("curl", "-sSLf", "https://virtuslab.github.io/scala-cli-packages/scala-setup.sh")
        .spawn(stderr = os.Inherit)

    // format: off
    os.proc(
      "sh", "-s", "--",
      "--force",
      "--binary-name", options.binaryName,
      "--bin-dir", options.installDirPath,
      updateToVersion
    ).call(
      cwd = os.pwd,
      stdin = installScript.stdout,
      stdout = os.Inherit
    ).out.text().trim
    // format: on
  }

  def update(options: UpdateOptions, scalaCliBinPath: os.Path) = {
    val currentVersion = Try {
      os.proc(scalaCliBinPath, "version").call(cwd = os.pwd).out.text().trim
    }.toOption.getOrElse("0.0.0")

    lazy val newestScalaCliVersion = {
      val resp =
        os.proc("curl", "--silent", "https://github.com/VirtusLab/scala-cli/releases/latest")
          .call(cwd = os.pwd)
          .out.text().trim

      val scalaCliVersionRegex = "tag/v(.*)\"".r
      scalaCliVersionRegex.findFirstMatchIn(resp).map(_.group(1))
    }.getOrElse(
      sys.error("Can not resolve ScalaCLI version to update")
    )

    if (!options.isInternalRun)
      if (CommandUtils.isOutOfDateVersion(newestScalaCliVersion, currentVersion))
        updateScalaCli(options, newestScalaCliVersion)
      else println("ScalaCLI is up-to-date")
    else
      println(
        s"""Your ScalaCLI $currentVersion is outdated, please update ScalaCLI to $newestScalaCliVersion

           |Run 'curl -sSLf https://virtuslab.github.io/scala-cli-packages/scala-setup.sh | sh' to update ScalaCLI.""".stripMargin
      )
  }

  def checkUpdate(options: UpdateOptions) = {

    val scalaCliBinPath = options.installDirPath / options.binaryName

    lazy val execScalaCliPath = os.proc("which", "scala-cli").call(cwd = os.pwd).out.text().trim
    lazy val isScalaCliInPath = // if binDir is non empty, we not except scala-cli in PATH, it is useful in tests
      execScalaCliPath.contains(options.installDirPath.toString()) || options.binDir.isDefined

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
    else update(options, scalaCliBinPath)
  }

  def run(options: UpdateOptions, args: RemainingArgs): Unit =
    checkUpdate(options)

  def checkUpdate(logger: Logger): Unit = {
    Try(
      checkUpdate(UpdateOptions(isInternalRun = true))
    ) match {
      case Failure(ex) =>
        logger.debug(s"Ignoring error during checking update: $ex")
      case Success(_) => ()
    }
  }
}
