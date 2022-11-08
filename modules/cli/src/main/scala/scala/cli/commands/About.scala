package scala.cli.commands

import caseapp.*

import scala.build.Logger
import scala.build.internal.Constants
import scala.cli.CurrentParams
import scala.cli.commands.util.CommonOps.*

object About extends ScalaCommand[AboutOptions] {

  override def group                                                         = "Miscellaneous"
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
