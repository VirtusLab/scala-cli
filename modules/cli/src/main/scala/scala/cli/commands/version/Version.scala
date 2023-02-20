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
  override def helpFormat: HelpFormat = super.helpFormat.copy(
    hiddenGroups = Some(Seq("Logging")),
    hiddenGroupsWhenShowHidden = Some(Seq("Logging")),
    sortedGroups = Some(Seq("Version"))
  )

  override def runCommand(options: VersionOptions, args: RemainingArgs, logger: Logger): Unit = {
    lazy val maybeNewerScalaCliVersion: Option[String] =
      Update.newestScalaCliVersion(options.ghToken.map(_.get())) match {
        case Left(e) =>
          logger.debug(e.message)
          None
        case Right(newestScalaCliVersion) =>
          if CommandUtils.isOutOfDateVersion(newestScalaCliVersion, Constants.version) then
            Some(newestScalaCliVersion)
          else None
      }
    if options.cliVersion then println(Constants.version)
    else if options.scalaVersion then println(Constants.defaultScalaVersion)
    else {
      println(versionInfo)
      if !options.offline then
        maybeNewerScalaCliVersion.foreach { v =>
          logger.message(
            s"""Your $fullRunnerName version is outdated. The newest version is $v
               |It is recommended that you update $fullRunnerName through the same tool or method you used for its initial installation for avoiding the creation of outdated duplicates.""".stripMargin
          )
        }
    }
  }

  private def versionInfo: String =
    val version            = Constants.version
    val detailedVersionOpt = Constants.detailedVersion.filter(_ != version).fold("")(" (" + _ + ")")
    s"""$fullRunnerName version: $version$detailedVersionOpt
       |Scala version (default): ${Constants.defaultScalaVersion}""".stripMargin
}
