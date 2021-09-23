package scala.build.preprocessing.directives

import scala.build.options.{BuildOptions, ScalaOptions}

case object UsingScalaVersionDirectiveHandler extends UsingDirectiveHandler {
  def handle(directive: Directive): Option[Either[String, BuildOptions]] =
    directive.values match {
      case Seq("scala", scalaVer) if scalaVer.nonEmpty =>
        val options = BuildOptions(
          scalaOptions = ScalaOptions(
            scalaVersion = Some(scalaVer)
          )
        )
        // TODO Validate that scalaVer looks like a version?
        Some(Right(options))
      case _ =>
        None
    }
}
