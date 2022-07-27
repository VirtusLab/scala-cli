package scala.cli.integration

trait TestScalaVersionArgs extends ScalaCliSuite {

  override def group: ScalaCliSuite.TestGroup =
    if (actualScalaVersion.startsWith("2.12.")) ScalaCliSuite.TestGroup.Third
    else if (actualScalaVersion.startsWith("2.13.")) ScalaCliSuite.TestGroup.Second
    else ScalaCliSuite.TestGroup.First

  def scalaVersionOpt: Option[String]

  lazy val scalaVersionArgs = scalaVersionOpt match {
    case None     => Nil
    case Some(sv) => Seq("--scala", sv)
  }

  lazy val actualScalaVersion = scalaVersionOpt.getOrElse(Constants.defaultScala)

}
