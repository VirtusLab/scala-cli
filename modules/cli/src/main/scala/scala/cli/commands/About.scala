package scala.cli.commands

import caseapp._

import scala.build.internal.Constants
import scala.cli.CurrentParams

class About(isSipScala: Boolean) extends ScalaCommand[AboutOptions] {
  override def group = "Miscellaneous"
  def run(options: AboutOptions, args: RemainingArgs): Unit = {
    CurrentParams.verbosity = options.verbosity.verbosity
    val version            = Constants.version
    val detailedVersionOpt = Constants.detailedVersion.filter(_ != version)
    val appName =
      if (isSipScala) "Scala command"
      else "Scala CLI"
    println(s"$appName version $version" + detailedVersionOpt.fold("")(" (" + _ + ")"))
    if (Version.isOutdated(None)) println(Update.updateInstructions)
  }
}
