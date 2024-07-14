package scala.cli.integration

abstract class ExportMavenTestDefinitions extends ScalaCliSuite
    with TestScalaVersionArgs with ExportCommonTestDefinitions with MavenTestHelper {
  _: TestScalaVersion & MavenLanguageMode =>
  override def exportCommand(args: String*): os.proc =
    os.proc(
      TestUtil.cli,
      "--power",
      "export",
      extraOptions,
      "--mvn",
      "-o",
      outputDir.toString,
      args
    )

  override def buildToolCommand(root: os.Path, args: String*): os.proc = mavenCommand(args*)

  // todo: not sure if this is right. how to get the main class name
  override val runMainArgs: Seq[String] =
    if (language == JAVA) Seq("exec:java", "-Dexec.mainClass=Main") else Seq("scala:run", "-DmainClass=Main")
  override val runTestsArgs: Seq[String] =
    if (language == JAVA) Seq("exec:java", "-Dexec.mainClass=Main") else Seq("scala:run", "-DmainClass=Main")

}

sealed trait Language
case object JAVA  extends Language
case object SCALA extends Language

sealed trait MavenLanguageMode {
  def language: Language
}

trait MavenJava extends MavenLanguageMode {
  final override def language: Language = JAVA
}

trait MavenScala extends MavenLanguageMode {
  final override def language: Language = SCALA
}
