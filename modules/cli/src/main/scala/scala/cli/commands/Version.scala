package scala.cli.commands

import caseapp.*

import scala.build.internal.Constants
import scala.cli.CurrentParams

class Version(isSipScala: Boolean) extends ScalaCommand[VersionOptions] {
  override def group = "Miscellaneous"
  override def verbosity(options: VersionOptions): Option[Int] =
    Some(options.verbosity.verbosity)
  override def runCommand(options: VersionOptions, args: RemainingArgs): Unit = {
    if (options.cliVersion)
      println(Constants.version)
    else if (options.scalaVersion)
      println(Constants.defaultScalaVersion)
    else
      println(Version.versionInfo(isSipScala))
  }
}

object Version {
  def versionInfo(isSipScala: Boolean): String =
    val version            = Constants.version
    val detailedVersionOpt = Constants.detailedVersion.filter(_ != version).fold("")(" (" + _ + ")")
    val appName =
      if (isSipScala) "Scala code runner"
      else "Scala CLI"
    s"""$appName version: $version$detailedVersionOpt
       |Scala version (default): ${Constants.defaultScalaVersion}""".stripMargin
}
