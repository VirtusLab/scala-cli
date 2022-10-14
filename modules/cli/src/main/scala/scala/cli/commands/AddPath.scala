package scala.cli.commands

import caseapp.*
import coursier.env.{EnvironmentUpdate, ProfileUpdater}

import java.io.File

import scala.cli.CurrentParams
import scala.cli.commands.util.CommonOps.*
import scala.util.Properties

object AddPath extends ScalaCommand[AddPathOptions] {
  override def hidden       = true
  override def isRestricted = true
  override def loggingOptions(options: AddPathOptions): Option[LoggingOptions] =
    Some(options.logging)
  override def runCommand(options: AddPathOptions, args: RemainingArgs): Unit = {
    val logger = options.logging.logger
    if args.all.isEmpty then logger.error("Nothing to do")
    else {
      val update = EnvironmentUpdate(Nil, Seq("PATH" -> args.all.mkString(File.pathSeparator)))
      val didUpdate =
        if (Properties.isWin) {
          val updater = CustomWindowsEnvVarUpdater().withUseJni(Some(coursier.paths.Util.useJni()))
          updater.applyUpdate(update)
        }
        else {
          val updater = ProfileUpdater()
          updater.applyUpdate(update, Some(options.title).filter(_.nonEmpty))
        }
      if !didUpdate then logger.log("Everything up-to-date")
    }
  }
}
