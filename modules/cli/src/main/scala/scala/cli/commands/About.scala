package scala.cli.commands

import caseapp._

import scala.cli.CurrentParams

class About(appName: String) extends ScalaCommand[AboutOptions] {
  override def group = "Miscellaneous"
  def run(options: AboutOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.verbosity.verbosity
    println(s"$appName version ${Version.version}" + Version.detailedVersionOpt.fold("")(
      " detailed version: (" + _ + ")"
    ))
    if (Version.isOutdated(None)) println(Update.updateInstructions)
  }
}
