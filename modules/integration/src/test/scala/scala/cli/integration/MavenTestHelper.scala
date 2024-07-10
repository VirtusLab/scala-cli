package scala.cli.integration

trait MavenTestHelper {

  protected def mavenCommand(args: String*): os.proc = os.proc(maven, args)

  protected lazy val maven: os.Shellable =
    Seq[os.Shellable](
      "mvn",
      "clean",
      "compile",
      "exec:java"
    )
}
