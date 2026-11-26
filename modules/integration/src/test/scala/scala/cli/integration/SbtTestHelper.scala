package scala.cli.integration

import scala.cli.integration.TestUtil.stableStdoutOf
import scala.util.Properties

trait SbtTestHelper {
  private def fetchSbtLaunchJar(sbtVersion: String): os.Path =
    val res =
      stableStdoutOf(
        TestUtil.cs,
        "fetch",
        "--intransitive",
        s"org.scala-sbt:sbt-launch:$sbtVersion"
      )()
    val rawPath = res
    val path    = os.Path(rawPath, os.pwd)
    if os.isFile(path) then path
    else sys.error(s"Something went wrong (invalid sbt launch JAR path '$rawPath')")

  private lazy val sbt1LaunchJar: os.Path = fetchSbtLaunchJar(Constants.sbt1Version)
  private lazy val sbt2LaunchJar: os.Path = fetchSbtLaunchJar(Constants.sbt2Version)

  protected def sbtLaunchJar(sbtVersion: String): os.Path =
    if sbtVersion == Constants.sbt1Version then sbt1LaunchJar
    else if sbtVersion == Constants.sbt2Version then sbt2LaunchJar
    else fetchSbtLaunchJar(sbtVersion)

  private def sbtJavaCommand(sbtVersion: String): Seq[String] =
    Seq(
      "java",
      "-Xmx512m",
      "-Xms128m",
      "-Djline.terminal=jline.UnsupportedTerminal",
      "-Dsbt.log.noformat=true",
      "-jar",
      sbtLaunchJar(sbtVersion).toPlatformString
    )

  // The Windows CI jobs set COURSIER_JNI=force, which makes the coursier shaded in sbt-launch
  // fail hard when loading its JNI DLL, which sbt-launch doesn't ship.
  private def windowsSbtLauncher(sbtVersion: String): os.Path =
    os.temp(
      s"""@echo off
         |set COURSIER_JNI=false
         |${sbtJavaCommand(sbtVersion).map(arg => s"\"$arg\"").mkString(" ")} %*
         |exit /b %ERRORLEVEL%
         |""".stripMargin,
      prefix = "sbt-",
      suffix = ".bat"
    )

  protected def sbtShellable(sbtVersion: String): os.Shellable =
    if Properties.isWin then windowsSbtLauncher(sbtVersion)
    else sbtJavaCommand(sbtVersion)

  protected def sbtCommand(sbtVersion: String, args: String*): os.proc =
    os.proc(sbtShellable(sbtVersion), args)
}
