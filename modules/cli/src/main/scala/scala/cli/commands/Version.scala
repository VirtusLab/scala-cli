package scala.cli.commands

import caseapp._

import scala.build.internal.Constants
import scala.cli.CurrentParams
import scala.cli.internal.ProcUtil

class Version(appName: String) extends ScalaCommand[VersionOptions] {
  override def group = "Miscellaneous"
  def run(options: VersionOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.verbosity.verbosity
    printVersionInfo
  }

  def printVersionInfo = {
    println(s"$appName version ${Version.version}" + Version.detailedVersionOpt.fold("")(
      " detailed version: (" + _ + ")"
    ))
    if (Version.isOutdated) println(Update.updateInstructions)
    Update.hasPathDuplicates match {
      case Left(error) => println(
          "scala-cli was not able to find its own instance(s) on your PATH due to the following error:\n" + error
        )
      case Right(hasPathDuplicates) => if (hasPathDuplicates) println(Update.pathDuplicatesWarning)
    }
  }
}

object Version {

  val version            = Constants.version
  val detailedVersionOpt = Constants.detailedVersion.filter(_ != version)
  lazy val newestScalaCliVersion = {
    val resp = ProcUtil.downloadFile("https://github.com/VirtusLab/scala-cli/releases/latest")

    val scalaCliVersionRegex = "tag/v(.*?)\"".r
    scalaCliVersionRegex.findFirstMatchIn(resp).map(_.group(1))
  }.getOrElse(
    sys.error("Can not resolve ScalaCLI version to update")
  )

  lazy val isOutdated = CommandUtils.isOutOfDateVersion(newestScalaCliVersion, Constants.version)
}
