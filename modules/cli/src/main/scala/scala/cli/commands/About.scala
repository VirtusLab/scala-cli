package scala.cli.commands

import caseapp._

import scala.build.internal.Constants
import scala.cli.CurrentParams

object About extends ScalaCommand[AboutOptions] {
  override def group = "Miscellaneous"
  def run(options: AboutOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.verbosity.verbosity
    val version            = Constants.version
    val detailedVersionOpt = Some(Constants.detailedVersion).filter(_ != version)
    println(s"Scala CLI version $version" + detailedVersionOpt.fold("")(" (" + _ + ")"))
  }
}
