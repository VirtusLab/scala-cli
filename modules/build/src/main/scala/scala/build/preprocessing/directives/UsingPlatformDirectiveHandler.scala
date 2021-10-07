package scala.build.preprocessing.directives

import scala.build.Ops._
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
  def usage            = "using (jvm|scala-js|scala-native)+"
  override def usageMd = "`using `(`jvm`|`scala-js`|`scala-native`)+"
  override def examples = Seq(
    "using scala-js",
    "using jvm scala-native"
  )

  private def split(input: String): (String, Option[String]) = {
    val idx = input.indexOf(':')
    if (idx < 0) (input, None)
    else (input.take(idx), Some(input.drop(idx + 1)))
  }

  private def maybePlatforms(inputs: Seq[String]): Boolean =
    inputs.nonEmpty &&
    Platform.parse(Platform.normalize(split(inputs.head)._1)).nonEmpty

  def handle(directive: Directive): Option[Either[String, BuildOptions]] =
    directive.values match {
      case Seq(rawPfStrs @ _*) if maybePlatforms(rawPfStrs) =>
        val res = rawPfStrs
          .map { rawPfStr =>
            val (pfStr, pfVerOpt) = split(rawPfStr)
            Platform.parse(Platform.normalize(pfStr))
              .toRight(s"Unrecognized platform: $pfStr")
              .flatMap {
                case Platform.JVM =>
                  pfVerOpt match {
                    case None =>
                      val options = BuildOptions(
                        scalaOptions = ScalaOptions(
                          platform = Some(Platform.JVM)
                        )
                      )
                      Right(options)
                    case Some(_) =>
                      Left("Unexpected version specified for JVM platform")
                  }
                case Platform.JS =>
                  val options = BuildOptions(
                    scalaOptions = ScalaOptions(
                      platform = Some(Platform.JS)
                    ),
                    scalaJsOptions = ScalaJsOptions(
                      version = pfVerOpt
                    )
                  )
                  Right(options)
                case Platform.Native =>
                  val options = BuildOptions(
                    scalaOptions = ScalaOptions(
                      platform = Some(Platform.Native)
                    ),
                    scalaNativeOptions = ScalaNativeOptions(
                      version = pfVerOpt
                    )
                  )
                  Right(options)
              }
          }
          .sequence
          .left.map(_.mkString(", "))
          .map { options =>
            val merged    = options.foldLeft(BuildOptions())(_ orElse _)
            val platforms = options.flatMap(_.scalaOptions.platform.toSeq).distinct
            merged.copy(
              scalaOptions = merged.scalaOptions.copy(
                extraPlatforms = merged.scalaOptions.extraPlatforms ++ platforms.tail.toSet
              )
            )
          }
        Some(res)
      case _ =>
        None
    }
}
