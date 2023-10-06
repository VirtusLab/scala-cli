package scala.cli.commands.update

import caseapp.*
import caseapp.core.help.HelpFormat
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import coursier.core

import scala.build.Logger
import scala.build.errors.CheckScalaCliVersionError
import scala.build.internal.Constants.{ghName, ghOrg, version as scalaCliVersion}
import scala.cli.CurrentParams
import scala.cli.commands.shared.HelpGroup
import scala.cli.commands.{CommandUtils, ScalaCommand, SpecificationLevel}
import scala.cli.internal.ProcUtil
import scala.cli.signing.shared.Secret
import scala.cli.util.ArgHelpers.*
import scala.util.Properties
import scala.util.control.NonFatal

object Update extends ScalaCommand[UpdateOptions] {

  override def scalaSpecificationLevel: SpecificationLevel = SpecificationLevel.IMPLEMENTATION

  override def helpFormat: HelpFormat = super.helpFormat.withPrimaryGroup(HelpGroup.Update)

  private final case class Release(
    draft: Boolean,
    prerelease: Boolean,
    tag_name: String
  ) {
    lazy val version: core.Version =
      coursier.core.Version(tag_name.stripPrefix("v"))
    def actualRelease: Boolean =
      !draft && !prerelease
  }

  private lazy val releaseListCodec: JsonValueCodec[List[Release]] = JsonCodecMaker.make

  def newestScalaCliVersion(tokenOpt: Option[Secret[String]])
    : Either[CheckScalaCliVersionError, String] = {
    // FIXME Do we need paging here?
    val url = s"https://api.github.com/repos/$ghOrg/$ghName/releases"
    val headers =
      Seq("Accept" -> "application/vnd.github.v3+json") ++
        tokenOpt.toSeq.map(tk => "Authorization" -> s"token ${tk.value}")

    try {
      val resp = ProcUtil.download(url, headers: _*)
      readFromArray(resp)(releaseListCodec).filter(_.actualRelease)
        .maxByOption(_.version)
        .map(_.version.repr)
        .toRight(CheckScalaCliVersionError(s"No $fullRunnerName versions found in $url"))
    }
    catch {
      case e: JsonReaderException => Left(CheckScalaCliVersionError(s"Error reading $url", e))
      case e: Throwable => Left(CheckScalaCliVersionError(
          s"Failed to check for the newest Scala CLI version upstream: ${e.getMessage}",
          e
        ))
    }
  }

  def installDirPath(options: UpdateOptions): os.Path =
    options.binDir.map(os.Path(_, os.pwd)).getOrElse(
      scala.build.Directories.default().binRepoDir / options.binaryName
    )

  private def updateScalaCli(options: UpdateOptions, newVersion: String, logger: Logger): Unit = {
    val interactive =
      options.global.logging.verbosityOptions.interactiveInstance(forceEnable = true)
    if (!options.force) {
      val fallbackAction = () => {
        logger.error(s"To update $baseRunnerName to $newVersion pass -f or --force")
        sys.exit(1)
      }
      val msg = s"Do you want to update $baseRunnerName to version $newVersion?"
      interactive.confirmOperation(msg).getOrElse(fallbackAction())
    }

    val installationScript =
      ProcUtil.downloadFile("https://scala-cli.virtuslab.org/get")

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
    val output = res.out.trim()
    if (res.exitCode != 0) {
      logger.error(s"Error during updating $baseRunnerName: $output")
      sys.exit(1)
    }
  }

  private def getCurrentVersion(scalaCliBinPath: os.Path): String = {
    val res = os.proc(scalaCliBinPath, "version", "--cli-version").call(cwd = os.pwd, check = false)
    if (res.exitCode == 0)
      res.out.trim()
    else
      "0.0.0"
  }

  private def update(options: UpdateOptions, currentVersion: String, logger: Logger): Unit = {

    val newestScalaCliVersion0 = newestScalaCliVersion(options.ghToken.map(_.get())) match {
      case Left(e) =>
        logger.log(e.message)
        sys.error(e.message)
      case Right(v) => v
    }
    val isOutdated = CommandUtils.isOutOfDateVersion(newestScalaCliVersion0, currentVersion)

    if (!options.isInternalRun)
      if (isOutdated)
        updateScalaCli(options, newestScalaCliVersion0, logger)
      else println(s"$fullRunnerName is up-to-date")
    else if (isOutdated)
      println(
        s"""Your $fullRunnerName $currentVersion is outdated, please update $fullRunnerName to $newestScalaCliVersion0
           |Run 'curl -sSLf https://scala-cli.virtuslab.org/get | sh' to update $fullRunnerName.""".stripMargin
      )
  }

  override def runCommand(
    options: UpdateOptions,
    remainingArgs: RemainingArgs,
    logger: Logger
  ): Unit =
    checkUpdate(options, logger)

  def checkUpdate(options: UpdateOptions, logger: Logger): Unit = {
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
        logger.error(
          s"$fullRunnerName was not installed by the installation script, please use your package manager to update $baseRunnerName."
        )
        sys.exit(1)
      }
    }
    else if (Properties.isWin) {
      if (!options.isInternalRun) {
        logger.error(s"$fullRunnerName update is not supported on Windows.")
        sys.exit(1)
      }
    }
    else if (options.binaryName == baseRunnerName)
      update(options, scalaCliVersion, logger)
    else
      update(options, getCurrentVersion(scalaCliBinPath), logger)
  }

  def checkUpdateSafe(logger: Logger): Unit =
    try
      // log about update only if scala-cli was installed from installation script
      if (isScalaCLIInstalledByInstallationScript)
        checkUpdate(UpdateOptions(isInternalRun = true), logger)
    catch {
      case NonFatal(ex) =>
        logger.debug(s"Ignoring error during checking update: $ex")
    }

  def isScalaCLIInstalledByInstallationScript: Boolean = {
    val classesDir =
      getClass.getProtectionDomain.getCodeSource.getLocation.toURI.toString
    val binRepoDir = build.Directories.default().binRepoDir.toString()

    classesDir.contains(binRepoDir)
  }
}
