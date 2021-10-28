package scala.cli.commands

import caseapp._

import scala.build.Logger
import scala.util.{Failure, Success, Try}

object Update extends ScalaCommand[UpdateOptions] {

  private def isOutOfDate(newVersion: String, oldVersion: String): Boolean = {
    import coursier.core.Version

    Version(newVersion) > Version(oldVersion)
  }

  private def updateScalaCli(options: UpdateOptions) = {
    val installScript =
      os.proc("curl", "-sSLf", "https://virtuslab.github.io/scala-cli-packages/scala-setup.sh")
        .spawn(stderr = os.Inherit)

    os.proc("sh").call(cwd = os.pwd, stdin = installScript.stdout).out.text().trim
  }

  def run(options: UpdateOptions, args: RemainingArgs): Unit = {

    val installDir =
      options.binDirPath.getOrElse(scala.build.Directories.default().binRepoDir / "scala-cli")

    val scalaCliBinPath = installDir / options.binaryName

    if (!os.exists(scalaCliBinPath)) {
      System.err.println(
        s"Scala CLI was not installed by the installation script, please use your packager menager to update scala-cli."
      )
      sys.exit(1)
    }

    val currentVersion = Try {
      os.proc(scalaCliBinPath, "version").call(cwd = os.pwd).out.text().trim
    }.toOption.getOrElse("0.0.0")

    val newestScalaCliVersion = {
      val resp =
        os.proc("curl", "--silent", "https://github.com/VirtusLab/scala-cli/releases/latest")
          .call(cwd = os.pwd)
          .out.text().trim

      val scalaCliVersionRegex = "tag/v(.*)\"".r
      scalaCliVersionRegex.findFirstMatchIn(resp).map(_.group(1)).getOrElse("0.0.0")
    }

    if (options.force)
      if (isOutOfDate(newestScalaCliVersion, currentVersion))
        updateScalaCli(options)
      else
        println("scala-cli is up-to-date")
    else
      println(
        s"""Your scala-cli $currentVersion is outdated, please update scala-cli to $newestScalaCliVersion
           |Run 'curl -sSLf https://virtuslab.github.io/scala-cli-packages/scala-setup.sh | sh' to update scala-cli.""".stripMargin
      )
  }

  def checkUpdate(logger: Logger): Unit =
    Try(run(UpdateOptions(force = false), RemainingArgs(Nil, Nil))) match {
      case Failure(ex) =>
        logger.debug(s"Ignoring error during checking update: $ex")
      case Success(_) => ()
    }
}
