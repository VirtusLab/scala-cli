package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.Ops._
import scala.build.errors.{
  BuildException,
  CompositeBuildException,
  MalformedPlatformError,
  UnexpectedJvmPlatformVersionError
}
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

  private def handle(rawPfStrs: Seq[String]): Either[BuildException, BuildOptions] = either {
    val options = value {
      rawPfStrs
        .map { rawPfStr =>
          val (pfStr, pfVerOpt) = split(rawPfStr)
          Platform.parse(Platform.normalize(pfStr))
            .toRight(new MalformedPlatformError(pfStr))
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
                    Left(new UnexpectedJvmPlatformVersionError)
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
        .left.map(CompositeBuildException(_))
    }

    val merged    = options.foldLeft(BuildOptions())(_ orElse _)
    val platforms = options.flatMap(_.scalaOptions.platform.toSeq).distinct
    merged.copy(
      scalaOptions = merged.scalaOptions.copy(
        extraPlatforms = merged.scalaOptions.extraPlatforms ++ platforms.tail.toSet
      )
    )
  }

  def handle(directive: Directive): Option[Either[BuildException, BuildOptions]] =
    directive.values match {
      case Seq(rawPfStrs @ _*) if maybePlatforms(rawPfStrs) =>
        Some(handle(rawPfStrs))
      case _ =>
        None
    }

  override def keys = Seq("platform", "platforms")
  override def handleValues(values: Seq[Any]): Either[BuildException, BuildOptions] =
    handle(DirectiveUtil.stringValues(values))
}
