package scala.build.preprocessing.directives

import scala.build.options.{BuildOptions, Platform, ScalaJsOptions, ScalaNativeOptions}

case object UsingPlatformDirectiveHandler extends UsingDirectiveHandler {
  def name             = "Platform"
  def description      = "Set the default platform to Scala.JS or Scala Native"
  def usage            = "using scala-js|scala-native"
  override def usageMd = "`using scala-js`|`scala-native`"
  override def examples = Seq(
    "using scala-js",
    "using scala-native"
  )

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
