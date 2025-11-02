package scala.cli.integration

abstract class ExportMavenTestDefinitions extends ScalaCliSuite
    with TestScalaVersionArgs with ExportCommonTestDefinitions with MavenTestHelper {
  this: TestScalaVersion & MavenLanguageMode =>
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

  override def buildToolCommand(root: os.Path, mainClass: Option[String], args: String*): os.proc =
    mavenCommand(args*)

  override def runMainArgs(mainClass: Option[String]): Seq[String] = {
    require(mainClass.nonEmpty, "Main class or Test class is mandatory to build in maven")
    if (language == JAVA) Seq("exec:java", s"-Dexec.mainClass=${mainClass.get}")
    else Seq("scala:run", s"-DmainClass=${mainClass.get}")
  }

  override def runTestsArgs(mainClass: Option[String]): Seq[String] =
    if (language == JAVA) Seq("test")
    else Seq("test")

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
