package scala.cli.commands.about

import caseapp.*

import scala.build.Logger
import scala.build.internal.Constants
import scala.cli.CurrentParams
import scala.cli.commands.update.Update
import scala.cli.commands.version.Version
import scala.cli.commands.{CommandUtils, ScalaCommand}

object About extends ScalaCommand[AboutOptions] {

  override def group                                                         = "Miscellaneous"

  override def scalaSpecificationLevel                         = SpecificationLevel.IMPLEMENTATION
  

  override def runCommand(options: AboutOptions, args: RemainingArgs, logger: Logger): Unit = {
    println(Version.versionInfo)
    val newestScalaCliVersion = Update.newestScalaCliVersion(options.ghToken.map(_.get()))
    val isOutdated = CommandUtils.isOutOfDateVersion(newestScalaCliVersion, Constants.version)
    if (isOutdated)
      logger.message(
        s"""Your $fullRunnerName. version is outdated. The newest version is $newestScalaCliVersion
           |It is recommended that you update $fullRunnerName through the same tool or method you used for its initial installation for avoiding the creation of outdated duplicates.""".stripMargin
      )
  }
}
