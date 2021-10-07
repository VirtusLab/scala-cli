package scala.cli.commands

import caseapp._

import scala.build.internal.Constants

object Version extends ScalaCommand[VersionOptions] {
  override def group = "Miscellaneous"
  def run(options: VersionOptions, args: RemainingArgs): Unit = {
    println(Constants.version)
  }
}
