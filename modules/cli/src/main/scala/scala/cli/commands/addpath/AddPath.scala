package scala.cli.commands.addpath

import caseapp.*
import coursier.env.{EnvironmentUpdate, ProfileUpdater}

import java.io.File

import scala.build.Logger
import scala.cli.CurrentParams
import scala.cli.commands.util.CommonOps.*
import scala.cli.commands.{AddPathOptions, CustomWindowsEnvVarUpdater, ScalaCommand}
import scala.util.Properties

object AddPath extends ScalaCommand[AddPathOptions] {
  override def hidden                                          = true
  override def scalaSpecificationLevel                         = SpecificationLevel.RESTRICTED
  override def runCommand(options: AddPathOptions, args: RemainingArgs, logger: Logger): Unit = {
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
