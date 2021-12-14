package scala.cli.commands

import caseapp._

import scala.build.internal.Constants
import scala.cli.CurrentParams

object Version extends ScalaCommand[VersionOptions] {
  override def group = "Miscellaneous"
  def run(options: VersionOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.verbosity.verbosity
    println(Constants.version)
  }
}
