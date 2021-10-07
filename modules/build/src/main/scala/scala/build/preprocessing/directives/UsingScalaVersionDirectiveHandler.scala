package scala.build.preprocessing.directives

import scala.build.options.{BuildOptions, ScalaOptions}

case object UsingScalaVersionDirectiveHandler extends UsingDirectiveHandler {
  def name             = "Scala version"
  def description      = "Sets the default Scala version"
  def usage            = "using scala _version_+"
  override def usageMd = "`using scala `_version_+"
  override def examples = Seq(
    "using scala 3.0.2",
    "using scala 2.13",
    "using scala 2",
    "using scala 2.13.6 2.12.15"
  )

  def handle(directive: Directive): Option[Either[String, BuildOptions]] =
    directive.values match {
      case Seq("scala", scalaVersions @ _*) if scalaVersions.nonEmpty =>
        val options = BuildOptions(
          scalaOptions = ScalaOptions(
            scalaVersion = Some(scalaVersions.head),
            extraScalaVersions = scalaVersions.tail.toSet
          )
        )
        // TODO Validate that scalaVer looks like a version?
        Some(Right(options))
      case _ =>
        None
    }
}
