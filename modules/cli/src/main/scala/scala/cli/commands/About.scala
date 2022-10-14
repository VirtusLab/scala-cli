package scala.cli.commands

import caseapp.*

import scala.build.internal.Constants
import scala.cli.CurrentParams
import scala.cli.commands.util.CommonOps.*

class About(isSipScala: Boolean) extends ScalaCommand[AboutOptions] {

  override def group                                                         = "Miscellaneous"
  override def loggingOptions(options: AboutOptions): Option[LoggingOptions] = Some(options.logging)
  override def runCommand(options: AboutOptions, args: RemainingArgs): Unit = {
    val logger = options.logging.logger
    println(Version.versionInfo(isSipScala))
    val newestScalaCliVersion = Update.newestScalaCliVersion(options.ghToken.map(_.get()))
    val isOutdated = CommandUtils.isOutOfDateVersion(newestScalaCliVersion, Constants.version)
    if (isOutdated)
      logger.message(
        s"""Your Scala CLI version is outdated. The newest version is $newestScalaCliVersion
           |It is recommended that you update Scala CLI through the same tool or method you used for its initial installation for avoiding the creation of outdated duplicates.""".stripMargin
      )
  }
}
