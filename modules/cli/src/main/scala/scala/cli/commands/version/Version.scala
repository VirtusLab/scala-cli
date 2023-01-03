package scala.cli.commands.version

import caseapp.*
import caseapp.core.help.HelpFormat

import scala.build.Logger
import scala.build.internal.Constants
import scala.cli.CurrentParams
import scala.cli.commands.update.Update
import scala.cli.commands.{CommandUtils, ScalaCommand}
import scala.cli.config.PasswordOption

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
    lazy val newestScalaCliVersion = Update.newestScalaCliVersion(options.ghToken.map(_.get()))
    lazy val isVersionOutOfDate: Boolean =
      CommandUtils.isOutOfDateVersion(newestScalaCliVersion, Constants.version)
    if options.cliVersion then println(Constants.version)
    else if options.scalaVersion then println(Constants.defaultScalaVersion)
    else if !options.offline && isVersionOutOfDate then {
      println(versionInfo)
      logger.message(
        s"""Your $fullRunnerName version is outdated. The newest version is $newestScalaCliVersion
           |It is recommended that you update $fullRunnerName through the same tool or method you used for its initial installation for avoiding the creation of outdated duplicates.""".stripMargin
      )
    }
    else println(versionInfo)
  }

  private def versionInfo: String =
    val version            = Constants.version
    val detailedVersionOpt = Constants.detailedVersion.filter(_ != version).fold("")(" (" + _ + ")")
    s"""$fullRunnerName version: $version$detailedVersionOpt
       |Scala version (default): ${Constants.defaultScalaVersion}""".stripMargin
}
