package scala.cli.integration

trait MavenTestHelper {

  protected def mavenCommand(args: String*): os.proc = os.proc(maven, args)
  protected def mavenJavaCommand(args: String*): os.proc = os.proc(mavenJava, args)

  protected lazy val maven: os.Shellable =
    Seq[os.Shellable](
      "mvn",
      "clean",
      "compile",
      "scala:run"
    )

  protected lazy val mavenJava: os.Shellable =
    Seq[os.Shellable](
      "mvn",
      "clean",
      "compile",
      "exec:java"
    )
}
