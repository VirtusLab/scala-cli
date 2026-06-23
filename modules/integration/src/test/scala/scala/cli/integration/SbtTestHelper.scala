package scala.cli.integration

trait SbtTestHelper {
  private def fetchSbtLaunchJar(sbtVersion: String): os.Path =
    val res =
      os.proc(
        TestUtil.cs,
        "fetch",
        "--intransitive",
        s"org.scala-sbt:sbt-launch:$sbtVersion"
      ).call()
    val rawPath = res.out.trim()
    val path    = os.Path(rawPath, os.pwd)
    if os.isFile(path) then path
    else sys.error(s"Something went wrong (invalid sbt launch JAR path '$rawPath')")

  private lazy val sbt1LaunchJar: os.Path = fetchSbtLaunchJar(Constants.sbt1Version)
  private lazy val sbt2LaunchJar: os.Path = fetchSbtLaunchJar(Constants.sbt2Version)

  protected def sbtLaunchJar(sbtVersion: String): os.Path =
    if sbtVersion == Constants.sbt1Version then sbt1LaunchJar
    else if sbtVersion == Constants.sbt2Version then sbt2LaunchJar
    else fetchSbtLaunchJar(sbtVersion)

  protected def sbtShellable(sbtVersion: String): os.Shellable =
    Seq[os.Shellable](
      "java",
      "-Xmx512m",
      "-Xms128m",
      "-Djline.terminal=jline.UnsupportedTerminal",
      "-Dsbt.log.noformat=true",
      "-jar",
      sbtLaunchJar(sbtVersion)
    )

  protected def sbtCommand(sbtVersion: String, args: String*): os.proc =
    os.proc(sbtShellable(sbtVersion), args)
}
