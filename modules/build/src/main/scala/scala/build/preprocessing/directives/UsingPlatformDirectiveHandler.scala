package scala.build.preprocessing.directives

import scala.build.options.{
  BuildOptions,
  Platform,
  ScalaJsOptions,
  ScalaNativeOptions,
  ScalaOptions
}

case object UsingPlatformDirectiveHandler extends UsingDirectiveHandler {
  def name             = "Platform"
  def description      = "Set the default platform to Scala.JS or Scala Native"
  def usage            = "using scala-js|scala-native"
  override def usageMd = "`using scala-js`|`scala-native`"
  override def examples = Seq(
    "using scala-js",
    "using scala-native"
  )

  def handle(directive: Directive): Option[Either[String, BuildOptions]] =
    directive.values match {
      case Seq(pfName) =>
        Platform.parse(Platform.normalize(pfName)).map { pf =>
          val options = BuildOptions(
            scalaOptions = ScalaOptions(
              platform = Some(pf)
            )
          )
          Right(options)
        }
      case Seq(pfName, pfVersion) =>
        Platform.parse(Platform.normalize(pfName)).map {
          case Platform.JVM =>
            Left("Unexpected version specified for JVM platform")
          case Platform.JS =>
            val options = BuildOptions(
              scalaOptions = ScalaOptions(
                platform = Some(Platform.JS)
              ),
              scalaJsOptions = ScalaJsOptions(
                version = Some(pfVersion)
              )
            )
            Right(options)
          case Platform.Native =>
            val options = BuildOptions(
              scalaOptions = ScalaOptions(
                platform = Some(Platform.Native)
              ),
              scalaNativeOptions = ScalaNativeOptions(
                version = Some(pfVersion)
              )
            )
            Right(options)
        }
      case _ =>
        None
    }
}
