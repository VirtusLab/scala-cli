package scala.cli.integration

sealed trait TestBuildServer {
  def buildServerOptions: Seq[String]
  def buildServerDescriptionSuffix: String
  def usesBloop: Boolean = buildServerOptions.isEmpty
}
trait TestWithBloop extends TestBuildServer {
  override def buildServerOptions: Seq[String]      = Nil
  override def buildServerDescriptionSuffix: String = "with Bloop"
}
trait TestWithoutBloop extends TestBuildServer {
  override def buildServerOptions: Seq[String]      = Seq("--server=false")
  override def buildServerDescriptionSuffix: String = "without build server"
}
