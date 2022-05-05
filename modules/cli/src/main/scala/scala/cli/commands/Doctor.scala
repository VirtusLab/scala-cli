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
    // checkBinaryOrMainClass()??
    checkAccessToMavneOrGithub()
    checkIsNativeOrJvm()

    println("") // sometimes last line is cut off
  }

  private def checkIsVersionOutdated(): Unit = {
    val currentVersion = Constants.version
    val isOutdated = CommandUtils.isOutOfDateVersion(Update.newestScalaCliVersion, currentVersion)
    if (isOutdated)
      println(
        s"Your scala-cli version is out of date. your version: $currentVersion. please update to: ${Update.newestScalaCliVersion}"
      )
    else
      println(s"Your scala-cli version ($currentVersion) is current.")
  }

  private def checkBloopStatus(): Unit = {
    // TODO
  }

  // the semantics of PATH isn't just built into unix shells.  it is
  // part of the 'exec' series of system calls.
  private def checkDuplicatesOnPath(): Unit = {
    import java.io.File.pathSeparator, java.io.File.pathSeparatorChar

    var path = System.getenv("PATH")
    val pwd  = os.pwd.toString

    // on unix & macs, an empty PATH counts as ".", the working directory
    if (path.length == 0)
      path = pwd
    else {
      // scala 'split' doesn't handle leading or trailing pathSeparators
      // correctly so expand them now.
      if (path.head == pathSeparatorChar) path = pwd + path
      if (path.last == pathSeparatorChar) path = path + pwd
      // on unix and macs, an empty PATH item is like "." (current dir).
      path = s"$pathSeparator$pathSeparator".r
        .replaceAllIn(path, pathSeparator + pwd + pathSeparator)
    }

    val scalaCliPaths = path
      .split(pathSeparator)
      .map(d => if (d == ".") pwd else d) // on unix a bare "." counts as the current dir
      .map(_ + "/scala-cli")
      .filter { f =>
        val p: os.Path = if f.startsWith("/") then os.Path(f) else os.RelPath(f).resolveFrom(os.pwd)
        os.isFile(p)
      }
      .toSet

    if (scalaCliPaths.size > 1)
      println(
        s"scala-cli would not be able to update itself since it is installed in multiple directories: ${scalaCliPaths.mkString(", ")}."
      )
    else
      println(
        s"scala-cli could update itself since it is correctly installed in only one location: $scalaCliPaths."
      )
  }

  private def checkNativeDependencies(): Unit = {
    // TODO
  }

  private def checkJSDependencies(): Unit = {
    // TODO
  }

  // checkBinaryOrMainClass()??

  private def checkAccessToMavneOrGithub(): Unit = {
    // TODO
  }

  private def checkIsNativeOrJvm(): Unit = {
    if (System.getProperty("org.graalvm.nativeimage.kind") == "executable")
      println("Your scala-cli is a native application.")
    else {
      val jvmVersion        = System.getProperty("java.vm.name")
      val javaVendorVersion = System.getProperty("java.vendor.version")
      println(
        s"Your scala-cli is using the java launcher with JVM: $jvmVersion ($javaVendorVersion)."
      )
    }
  }

}
