package scala.cli.commands

import caseapp.core.RemainingArgs

import scala.build.internal.Constants

object Doctor extends ScalaCommand[DoctorOptions] {
  override def group = "Doctor"

  def run(options: DoctorOptions, args: RemainingArgs): Unit = {
    val currentVersion = Constants.version
    val isOutdated = CommandUtils.isOutOfDateVersion(Update.newestScalaCliVersion, currentVersion)
    if (isOutdated)
      println(
        s"the version is oudated current version : $currentVersion please update to ${Update.newestScalaCliVersion}"
      )
    else
      println("the version is updated")

    println()
  }
}
