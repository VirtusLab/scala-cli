package scala.cli.integration

trait SbtTestHelper {
  protected lazy val sbtLaunchJar: os.Path = {
    val res =
      os.proc(TestUtil.cs, "fetch", "--intransitive", "org.scala-sbt:sbt-launch:1.5.5").call()
    val rawPath = res.out.trim()
    val path    = os.Path(rawPath, os.pwd)
    if (os.isFile(path)) path
    else sys.error(s"Something went wrong (invalid sbt launch JAR path '$rawPath')")
  }

  protected lazy val sbt: os.Shellable =
    Seq[os.Shellable](
      "java",
      "-Xmx512m",
      "-Xms128m",
      "-Djline.terminal=jline.UnsupportedTerminal",
      "-Dsbt.log.noformat=true",
      "-jar",
      sbtLaunchJar
    )

  protected def sbtCommand(args: String*): os.proc = os.proc(sbt, args)
}
