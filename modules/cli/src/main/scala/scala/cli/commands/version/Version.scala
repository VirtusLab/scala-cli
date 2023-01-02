package scala.cli.commands.version

import caseapp.*
import caseapp.core.help.HelpFormat

import scala.build.Logger
import scala.build.internal.Constants
import scala.cli.CurrentParams
import scala.cli.commands.ScalaCommand

object Version extends ScalaCommand[VersionOptions] {
  override def group = "Miscellaneous"

  override def scalaSpecificationLevel = SpecificationLevel.SHOULD
  override def hasFullHelp: Boolean    = false
  override def helpFormat: HelpFormat = super.helpFormat.copy(
    hiddenGroups = Some(Seq("Logging")),
    sortedGroups = Some(
      Seq(
        "Version",
        "Help"
      )
    )
  )

  override def runCommand(options: VersionOptions, args: RemainingArgs, logger: Logger): Unit = {
    if (options.cliVersion)
      println(Constants.version)
    else if (options.scalaVersion)
      println(Constants.defaultScalaVersion)
    else
      println(versionInfo)
  }

  def versionInfo: String =
    val version            = Constants.version
    val detailedVersionOpt = Constants.detailedVersion.filter(_ != version).fold("")(" (" + _ + ")")
    s"""$fullRunnerName version: $version$detailedVersionOpt
       |Scala version (default): ${Constants.defaultScalaVersion}""".stripMargin
}
