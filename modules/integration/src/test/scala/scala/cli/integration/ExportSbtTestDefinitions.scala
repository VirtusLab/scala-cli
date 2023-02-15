package scala.cli.integration

abstract class ExportSbtTestDefinitions(override val scalaVersionOpt: Option[String])
    extends ScalaCliSuite with TestScalaVersionArgs with ExportCommonTestDefinitions {

  private lazy val sbtLaunchJar = {
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

  override def exportCommand(args: String*): os.proc =
    os.proc(
      TestUtil.cli,
      "--power",
      "export",
      extraOptions,
      "--sbt",
      "-o",
      outputDir.toString,
      args
    )

  override def buildToolCommand(root: os.Path, args: String*): os.proc = os.proc(sbt, args)

  override val runMainArgs: Seq[String]  = Seq("run")
  override val runTestsArgs: Seq[String] = Seq("test")
}
