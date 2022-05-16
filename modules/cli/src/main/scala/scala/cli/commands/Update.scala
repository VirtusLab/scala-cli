package scala.cli.commands

import caseapp._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros._

import scala.build.Logger
import scala.build.internal.Constants.{ghName, ghOrg, version => scalaCliVersion}
import scala.cli.CurrentParams
import scala.cli.internal.ProcUtil
import scala.cli.signing.shared.Secret
import scala.io.StdIn.readLine
import scala.util.Properties
import scala.util.control.NonFatal

object Update extends ScalaCommand[UpdateOptions] {

  private final case class Release(
    draft: Boolean,
    prerelease: Boolean,
    tag_name: String
  ) {
    lazy val version =
      coursier.core.Version(tag_name.stripPrefix("v"))
    def actualRelease: Boolean =
      !draft && !prerelease
  }

  private lazy val releaseListCodec: JsonValueCodec[List[Release]] = JsonCodecMaker.make

  def newestScalaCliVersion(tokenOpt: Option[Secret[String]]) = {

    // FIXME Do we need paging here?
    val url = s"https://api.github.com/repos/$ghOrg/$ghName/releases"
    val headers =
      Seq("Accept" -> "application/vnd.github.v3+json") ++
        tokenOpt.toSeq.map(tk => "Authorization" -> s"token ${tk.value}")
    val resp = ProcUtil.download(url, headers: _*)

    val releases =
      try readFromArray(resp)(releaseListCodec)
      catch {
        case e: JsonReaderException =>
          throw new Exception(s"Error reading $url", e)
      }

    releases
      .filter(_.actualRelease)
      .maxByOption(_.version)
      .map(_.version.repr)
      .getOrElse {
        sys.error(s"No Scala CLI versions found in $url")
      }
  }

  def installDirPath(options: UpdateOptions): os.Path =
    options.binDir.map(os.Path(_, os.pwd)).getOrElse(
      scala.build.Directories.default().binRepoDir / options.binaryName
    )

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
      "--bin-dir", installDirPath(options),
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

  private def getCurrentVersion(scalaCliBinPath: os.Path): String = {
    val res = os.proc(scalaCliBinPath, "version").call(cwd = os.pwd, check = false)
    if (res.exitCode == 0)
      res.out.text().trim
    else
      "0.0.0"
  }

  private def update(options: UpdateOptions, currentVersion: String): Unit = {

    val newestScalaCliVersion0 = newestScalaCliVersion(options.ghToken.map(_.get()))
    val isOutdated = CommandUtils.isOutOfDateVersion(newestScalaCliVersion0, currentVersion)

    if (!options.isInternalRun)
      if (isOutdated)
        updateScalaCli(options, newestScalaCliVersion0)
      else println("Scala CLI is up-to-date")
    else if (isOutdated)
      println(
        s"""Your Scala CLI $currentVersion is outdated, please update Scala CLI to $newestScalaCliVersion0
           |Run 'curl -sSLf https://virtuslab.github.io/scala-cli-packages/scala-setup.sh | sh' to update Scala CLI.""".stripMargin
      )
  }

  def checkUpdate(options: UpdateOptions) = {

    val scalaCliBinPath = installDirPath(options) / options.binaryName

    val programName = argvOpt.flatMap(_.headOption).getOrElse {
      sys.error("update called in a non-standard way :|")
    }

    lazy val isScalaCliInPath = // if binDir is non empty, we not except scala-cli in PATH, it is useful in tests
      CommandUtils.getAbsolutePathToScalaCli(programName).contains(
        installDirPath(options).toString()
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
        System.err.println("Scala CLI update is not supported on Windows.")
        sys.exit(1)
      }
    }
    else if (options.binaryName == "scala-cli") update(options, scalaCliVersion)
    else
      update(options, getCurrentVersion(scalaCliBinPath))
  }

  def run(options: UpdateOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.verbosity.verbosity
    checkUpdate(options)
  }

  def checkUpdateSafe(logger: Logger): Unit =
    try {
      val classesDir =
        getClass.getProtectionDomain.getCodeSource.getLocation.toURI.toString
      val binRepoDir = build.Directories.default().binRepoDir.toString()
      // log about update only if scala-cli was installed from installation script
      if (classesDir.contains(binRepoDir))
        checkUpdate(UpdateOptions(isInternalRun = true))
    }
    catch {
      case NonFatal(ex) =>
        logger.debug(s"Ignoring error during checking update: $ex")
    }
}
