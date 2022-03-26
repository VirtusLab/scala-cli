package scala.cli.commands

import caseapp.core.RemainingArgs

import scala.build.internal.Constants
import scala.tools.nsc.io.File

// current version / latest version + potentially information that
// scala-cli should be updated (and that should take SNAPSHOT version
// into account and mention that one is ahead of stable version)

// the state of bloop (running/not running + version and JVM used)

// if there are duplicated scala-cli on classpath

// whether all native dependencies for native / js are installed

// information about location of binary / main class that is being used

// information if scala-cli can access Maven central / scala-cli
// github with some tips and diagnostics about proxies
// (@alexarchambault could you provide more details on what can be
// printed?)

// information if scala-cli is used as a native application or is using JVM


object Doctor extends ScalaCommand[DoctorOptions] {
  override def group = "Doctor"

  def run(options: DoctorOptions, args: RemainingArgs): Unit = {
    checkIsVersionOutdated()
    checkBloopStatus()
    checkDuplicatesOnPath()
    checkNativeDependencies()
    checkJSDependencies()
    //checkBinaryOrMainClass()??
    checkAccessToMavneOrGithub()
    checkIsNativeOrJvm()
    println("invisible!")
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
  }

  private def checkBloopStatus(): Unit = {
    // TODO
  }

  private def checkDuplicatesOnPath(): Unit = {
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
  }

  private def checkNativeDependencies(): Unit = {
  }

  private def checkJSDependencies(): Unit = {
  }

    //checkBinaryOrMainClass()??

  private def checkAccessToMavneOrGithub(): Unit = {
  }

  private def checkIsNativeOrJvm(): Unit = {
    val jvmVersion = System.getProperty("java.vm.name")

    if (jvmVersion.isEmpty)
      println("scala-cli is used as a native application")
    else
      println(s"scala-cli using JVM : $jvmVersion")
  }

}


