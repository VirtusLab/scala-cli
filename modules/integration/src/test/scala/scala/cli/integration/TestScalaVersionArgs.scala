package scala.cli.integration

trait TestScalaVersionArgs {

  def scalaVersionOpt: Option[String]

  lazy val scalaVersionArgs = scalaVersionOpt match {
    case None     => Nil
    case Some(sv) => Seq("--scala", sv)
  }

  lazy val actualScalaVersion = scalaVersionOpt.getOrElse(Constants.defaultScala)

}
