package scala.cli.commands

import caseapp.core.RemainingArgs
import scala.build.internal.Constants

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
    import java.io.File.pathSeparator, java.io.File.pathSeparatorChar
    var path = System.getenv("PATH")

    // on unix & macs, an empty PATH counts as ".", the working directory
    if (path.length == 0) {
      path = os.pwd.toString
    } else {
      // scala 'split' doesn't handle leading or trailing pathSeparators
      // correctly so expand them now.
      if (path.head == pathSeparatorChar) { path = os.pwd.toString + path }
      if (path.last == pathSeparatorChar) { path = path + os.pwd.toString }
      // on unix and macs, an empty PATH item is like "." (current dir).
      path = s"${pathSeparator}${pathSeparator}".r.replaceAllIn(path, os.pwd.toString)
    }

    val directories: Array[String] = path
      .split(pathSeparator)
    // on unix & macs, a bare "." counts as the current dir
      .map(d => if (d == ".") os.pwd.toString else d)

    val scalaCliPaths = directories
      .map(_ + "/scala-cli")
      .filter { f => os.isFile(os.Path(f)) }
      .toSet

    if (scalaCliPaths.size > 1)
      println(s"scala-cli installed on multiple paths ${scalaCliPaths.mkString(", ")} ")
    else
      println("scala-cli installed correctly (only one instance in your PATH)")
  }

  private def checkNativeDependencies(): Unit = {
    // TODO
  }

  private def checkJSDependencies(): Unit = {
    // TODO
  }

  //checkBinaryOrMainClass()??

  private def checkAccessToMavneOrGithub(): Unit = {
    // TODO
  }

  private def checkIsNativeOrJvm(): Unit = {
    val jvmVersion = System.getProperty("java.vm.name")

    if (jvmVersion.isEmpty)
      println("scala-cli is used as a native application")
    else
      println(s"scala-cli using JVM : $jvmVersion")
  }

}


