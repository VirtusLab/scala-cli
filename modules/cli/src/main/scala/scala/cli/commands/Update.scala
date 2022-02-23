package scala.cli.commands

import caseapp._

import scala.build.EitherCps.{either, value}
import scala.build.Logger
import scala.build.internal.Constants
import scala.cli.CurrentParams
import scala.cli.commands.Version.{isOutdated, newestScalaCliVersion}
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

  /** Either an error or a boolean indicating whether there are duplicates for scala-cli on PATH
    */
  lazy val hasPathDuplicates: Either[String, Boolean] = either {
    val instances = value(pathInstances)
    instances.length > 1
  }

  lazy val updateInstructions: String = {
    val base = s"""Your Scala CLI version is outdated. The newest version is $newestScalaCliVersion
                  |It is recommended that you update Scala CLI through the same tool or method you used for its initial installation for avoiding the creation of outdated duplicates.""".stripMargin
    if (pathInstances.isRight)
      pathDuplicatesWarning.right.get
    else base
  }

  /** Either an error or the warning about more than one instance of scala-cli on PATH
    */
  lazy val pathDuplicatesWarning: Either[String, String] = either {
    s"""These are the instances of Scala CLI on your PATH:
       |${value(pathInstances).mkString("\n")}
       |If you have more than one instance, it is recommended to only keep the most recent version.""".stripMargin
  }

  def update(options: UpdateOptions, maybeScalaCliBinPath: Option[os.Path]): Unit = {
    val maybeCurrentVersion = maybeScalaCliBinPath.map {
      scalaCliBinPath =>
        val res = os.proc(scalaCliBinPath, "version").call(cwd = os.pwd, check = false)
        if (res.exitCode == 0)
          res.out.text().trim
        else
          "0.0.0"
    }

    val currentVersion = maybeCurrentVersion.getOrElse(Constants.version)

    val maybeOutDated =
      maybeCurrentVersion.map(CommandUtils.isOutOfDateVersion(newestScalaCliVersion, _))

//    val scalaCliVersionRegex = "tag/v(.*?)\"".r
//    scalaCliVersionRegex.findFirstMatchIn(resp).map(_.group(1))
//      .getOrElse(
//        sys.error("Can not resolve ScalaCLI version to update")
//      )

    val outDated = maybeOutDated.getOrElse(isOutdated)

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

  /** Either an error or a Seq of the descriptions of scala-cli's PATH instances in linux
    */
  lazy val pathInstances: Either[String, List[String]] = {
    val scalaCliPathInstances = os.proc("type", "-ap", "scala-cli").call(
      cwd = os.pwd,
      mergeErrIntoOut = true,
      check = false
    )

    if (scalaCliPathInstances.exitCode == 0)
      Right(scalaCliPathInstances.out.text().trim.split(sys.props("line.separator")).toList)
    else Left(scalaCliPathInstances.err.text().trim())
  }

  def checkUpdate(options: UpdateOptions): Unit = {

    val scalaCliBinPath = options.installDirPath / options.binaryName

    lazy val isScalaCliInPath = // if binDir is non empty, we do not expect scala-cli in PATH. But having it there is useful in tests
      CommandUtils.absolutePathToScalaCli.contains(
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
