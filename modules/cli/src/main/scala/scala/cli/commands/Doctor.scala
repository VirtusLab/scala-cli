package scala.cli.commands

import caseapp.core.RemainingArgs

import java.io.File
import scala.build.bloop.BloopThreads
import scala.build.blooprifle.BloopRifle
import scala.build.internal.Constants
import scala.util.Properties

// current version / latest version + potentially information that
// scala-cli should be updated (and that should take SNAPSHOT version
// into account and mention that one is ahead of stable version)

// the state of bloop (running/not running + version and JVM used)

// if there are duplicate scala-cli's on your PATH.

// whether all native dependencies for native / js are installed.

// information about location of binary / main class that is being used?

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
        s"Your scala-cli version is out of date. your version: $currentVersion. please update to: ${Update.newestScalaCliVersion}"
      )
    else
      println(s"Your scala-cli version ($currentVersion) is current.")
  }

  private def checkBloopStatus(): Unit = {
    val options = BloopStartOptions()
    val bloopRifleConfig = options.bloopRifleConfig()
    val logger           = options.logging.logger
    val workdir = new File(".").getCanonicalFile.toPath
    val isRunning = BloopRifle.check(bloopRifleConfig, logger.bloopRifleLogger)
    if (isRunning) {
      val threads = BloopThreads.create()
      val bloopInfoEither = BloopRifle.getCurrentBloopVersion(bloopRifleConfig,
        logger.bloopRifleLogger,
        workdir,
        threads.startServerChecks)
      val bloopVersion = bloopInfoEither.toOption.fold("couldn't retrieve")(_.bloopVersion.raw)
      println(s"Bloop is running (version $bloopVersion).")
    }
    else
      println("Bloop is not running")
  }

  // the semantics of PATH isn't just defined by unix shells.  it is
  // built into the 'exec' family of system calls (e.g. execlp,
  // execvp, and execvpe).
  private def checkDuplicatesOnPath(): Unit = {
    import java.io.File.pathSeparator, java.io.File.pathSeparatorChar

    var path = System.getenv("PATH")
    val pwd = os.pwd.toString

    // on unix & macs, an empty PATH counts as ".", the working directory.
    if (path.length == 0) {
      path = pwd
    } else {
      // scala 'split' doesn't handle leading or trailing pathSeparators
      // the way we need it to so expand them now by hand.
      if (path.head == pathSeparatorChar) { path = pwd + path }
      if (path.last == pathSeparatorChar) { path = path + pwd }
      // on unix and macs, an empty PATH item is like "." (current dir).
      path = s"${pathSeparator}${pathSeparator}".r
        .replaceAllIn(path, pathSeparator + pwd + pathSeparator)
    }

    val scalaCliPaths = path
      .split(pathSeparator)
      .map(d => if (d == ".") pwd else d) // on unix a bare "." counts as the current dir
      .flatMap(d =>
        if (Properties.isWin)
          List(d + "/scala-cli.bat", d + "/scala-cli.exe")
        else
          List(d + "/scala-cli"))
      .filter { f => os.isFile(os.Path(f)) }
      .toSet

    if (scalaCliPaths.size > 1)
      println(s"scala-cli would not be able to update itself since it is installed in multiple directories on your PATH: ${scalaCliPaths.mkString(", ")}.")
    else
      println(s"scala-cli could update itself since it is installed in only one directory on your PATH: ${scalaCliPaths}.")
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
    val javaVendorVersion = System.getProperty("java.vendor.version")

    if (scala.sys.props.contains("org.graalvm.nativeimage.imagecode"))
      println("Your scala-cli is a native application.")
    else
      println(s"Your scala-cli is using the java launcher with JVM: $jvmVersion ($javaVendorVersion).")
  }

}


