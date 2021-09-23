package scala.build.preprocessing.directives

import scala.build.options.{BuildOptions, Platform, ScalaJsOptions, ScalaNativeOptions}

case object UsingPlatformDirectiveHandler extends UsingDirectiveHandler {
  def handle(directive: Directive): Option[Either[String, BuildOptions]] = {

    val values       = directive.values
    val maybeOptions =
      // TODO Accept several platforms for cross-compilation
      if (values.lengthCompare(1) == 0)
        Platform.parse(Platform.normalize(values.head)).map {
          case Platform.JVM =>
            BuildOptions()
          case Platform.JS =>
            BuildOptions(
              scalaJsOptions = ScalaJsOptions(enable = true)
            )
          case Platform.Native =>
            BuildOptions(
              scalaNativeOptions = ScalaNativeOptions(enable = true)
            )
        }
      else
        None

    maybeOptions.map(Right(_))
  }
}
