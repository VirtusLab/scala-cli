package scala.cli.commands

import caseapp._
import coursier.env.{EnvironmentUpdate, ProfileUpdater}

import java.io.File

import scala.cli.CurrentParams
import scala.util.Properties

object AddPath extends ScalaCommand[AddPathOptions] {
  override def hidden = true
  def run(options: AddPathOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.verbosity

    if (args.all.isEmpty) {
      if (!options.quiet)
        System.err.println("Nothing to do")
    }
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
      if (!didUpdate && !options.quiet)
        System.err.println("Everything up-to-date")
    }
  }
}
