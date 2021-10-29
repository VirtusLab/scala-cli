package scala.cli.commands

import caseapp._

import scala.build.Logger
import scala.io.StdIn.readLine
import scala.util.{Failure, Properties, Success, Try}

object Update extends ScalaCommand[UpdateOptions] {

  private def isOutOfDate(newVersion: String, oldVersion: String): Boolean = {
    import coursier.core.Version

    Version(newVersion) > Version(oldVersion)
  }

  private def updateScalaCli(updateToVersion: String) = {
    if (coursier.paths.Util.useAnsiOutput()) {
      println(s"Do you want to update scala-cli to version $updateToVersion [Y/n]")
      val response = readLine()
      if (response != "Y") {
        System.err.println("Abort")
        sys.exit(1)
      }
    }

    val installScript =
      os.proc("curl", "-sSLf", "https://virtuslab.github.io/scala-cli-packages/scala-setup.sh")
        .spawn(stderr = os.Inherit)

    os.proc("sh", "-v", updateToVersion).call(
      cwd = os.pwd,
      stdin = installScript.stdout
    ).out.text().trim
  }

  def update(options: UpdateOptions, scalaCliBinPath: os.Path, isExternalRun: Boolean) = {
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

    val updateToVersion = options.version

    if (options.force && isExternalRun)
      if (updateToVersion.nonEmpty)
        updateScalaCli(updateToVersion.get)
      else if (isOutOfDate(newestScalaCliVersion, currentVersion))
        updateScalaCli(newestScalaCliVersion)
      else println("ScalaCLI is up-to-date")
    else
      println(
        s"""Your ScalaCLI $currentVersion is outdated, please update ScalaCLI to $newestScalaCliVersion

           |Run 'curl -sSLf https://virtuslab.github.io/scala-cli-packages/scala-setup.sh | sh' to update ScalaCLI.""".stripMargin
      )
  }

  def checkUpdate(options: UpdateOptions, isExternalRun: Boolean) = {
    val installDir =
      options.binDirPath.getOrElse(scala.build.Directories.default().binRepoDir / "scala-cli")
    val scalaCliBinPath = installDir / options.binaryName

    lazy val execScalaCliPath = os.proc("which", "scala-cli").call(cwd = os.pwd).out.text().trim

    if (!os.exists(scalaCliBinPath) || !execScalaCliPath.contains(installDir.toString())) {
      if (isExternalRun)
        System.err.println(
          "Scala CLI was not installed by the installation script, please use your package manager to update scala-cli."
        )
    }
    else if (Properties.isWin) {
      if (isExternalRun) System.err.println("ScalaCLI update is not supported on Windows.")
    }
    else update(options, scalaCliBinPath, isExternalRun)
  }

  def run(options: UpdateOptions, args: RemainingArgs): Unit =
    checkUpdate(options, isExternalRun = true)

  def checkUpdate(logger: Logger): Unit = {
    Try(
      checkUpdate(UpdateOptions(force = false), isExternalRun = false)
    ) match {
      case Failure(ex) =>
        logger.debug(s"Ignoring error during checking update: $ex")
      case Success(_) => ()
    }
  }
}
