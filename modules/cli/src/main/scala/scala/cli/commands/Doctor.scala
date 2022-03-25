package scala.cli.commands

import caseapp.core.RemainingArgs

import scala.build.internal.Constants
import scala.tools.nsc.io.File

object Doctor extends ScalaCommand[DoctorOptions] {
  override def group = "Doctor"

  def run(options: DoctorOptions, args: RemainingArgs): Unit = {
    checkIsVersionOutdated()
    checkMultiplePath()
    checkIsNativeOrJvm()
  }

  private def checkIsVersionOutdated(): Unit = {
    val currentVersion = Constants.version
    val isOutdated = CommandUtils.isOutOfDateVersion(Update.newestScalaCliVersion, currentVersion)
    if (isOutdated)
      println(
        s"the version is outdated current version : $currentVersion please update to ${Update.newestScalaCliVersion}"
      )
    else
      println("scala-cli version is updated")

    println()
  }

  private def checkIsNativeOrJvm(): Unit = {
    val jvmVersion = System.getProperty("java.vm.name")

    if (jvmVersion.isEmpty)
      println("scala-cli is used as a native application")
    else
      println(s"scala-cli using JVM : $jvmVersion")
    println()
  }

  private def checkMultiplePath(): Unit = {
    val directories: Array[String] = System.getenv("PATH")
      .split(File.pathSeparator)
      .filter(_.isEmpty)

    val scalaCliPaths = directories.filter { x =>
      val dirPath = os.Path(x)
      val scalaCliPath = dirPath / "scala-cli"

      os.exists(scalaCliPath)
    }.map(path => path + "/scala-cli")

    if (scalaCliPaths.length > 1)
      println(s"scala-cli installed on multiple paths ${scalaCliPaths.mkString(", ")} ")
    else
      println("scala-cli installed correctly")
    println()
  }
}


