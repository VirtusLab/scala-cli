package scala.cli.commands

import caseapp._
import scala.build.internal.Constants

object About extends Command[AboutOptions] {
  def run(options: AboutOptions, args: RemainingArgs): Unit = {
    val version = Constants.version
    val detailedVersionOpt = Some(Constants.detailedVersion).filter(_ != version)
    println(s"Scala CLI version $version" + detailedVersionOpt.fold("")(" (" + _ + ")"))
  }
}
