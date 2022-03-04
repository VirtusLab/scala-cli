package scala.cli.commands

import caseapp._

import scala.build.internal.Constants
import scala.cli.CurrentParams
import scala.cli.internal.ProcUtil

object Version extends ScalaCommand[VersionOptions] {
  override def group = "Miscellaneous"
  def run(options: VersionOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.verbosity.verbosity
    println(Version.version)
  }

  val version            = Constants.version
  val detailedVersionOpt = Constants.detailedVersion.filter(_ != version)
  lazy val newestScalaCliVersion = {
    val scalaCliVersionRegex = "tag/v(.*?)\"".r

    val resp = ProcUtil.downloadFile("https://github.com/VirtusLab/scala-cli/releases/latest")

    scalaCliVersionRegex.findFirstMatchIn(resp).map(_.group(1))
  }.getOrElse(
    sys.error("Can not resolve ScalaCLI version to update")
  )

  def isOutOfDateVersion(newVersion: String, oldVersion: String): Boolean =
    coursier.core.Version(newVersion) > coursier.core.Version(oldVersion)

  def isOutdated(maybeScalaCliBinPath: Option[os.Path]): Boolean =
    isOutOfDateVersion(newestScalaCliVersion, getCurrentVersion(maybeScalaCliBinPath))

  def getCurrentVersion(maybeScalaCliBinPath: Option[os.Path]): String = {
    val maybeCurrentVersion = maybeScalaCliBinPath.map {
      scalaCliBinPath =>
        val res = os.proc(scalaCliBinPath, "version").call(cwd = os.pwd, check = false)
        if (res.exitCode == 0)
          res.out.text().trim
        else
          "0.0.0"
    }
    maybeCurrentVersion.getOrElse(Constants.version)

  }

}
