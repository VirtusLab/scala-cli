package scala.cli.integration

trait MavenTestHelper {

  protected def mavenCommand(args: String*): os.proc = os.proc("ls")  //os.proc(sbt, args)
}
