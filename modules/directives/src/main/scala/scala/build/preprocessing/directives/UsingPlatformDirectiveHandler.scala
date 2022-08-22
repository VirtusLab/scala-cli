package scala.build.preprocessing.directives
import scala.build.Ops._
import scala.build.errors.{
  BuildException,
  CompositeBuildException,
  MalformedPlatformError,
  UnexpectedJvmPlatformVersionError
}
import scala.build.options._
import scala.build.{Logger, Positioned}

case object UsingPlatformDirectiveHandler extends UsingDirectiveHandler {
  def name             = "Platform"
  def description      = "Set the default platform to Scala.js or Scala Native"
  def usage            = "//> using platform (jvm|scala-js|scala-native)+"
  override def usageMd = "`//> using platform `(`jvm`|`scala-js`|`scala-native`)+"
  override def examples = Seq(
    "//> using platform \"scala-js\"",
    "//> using platform \"jvm\", \"scala-native\""
  )
  override def isRestricted = false

  private def split(input: String): (String, Option[String]) = {
    val idx = input.indexOf(':')
    if (idx < 0) (input, None)
    else (input.take(idx), Some(input.drop(idx + 1)))
  }

  private def constructBuildOptions(
    rawPfStrsWithPos: Seq[Positioned[String]]
  ): Either[BuildException, BuildOptions] =
    rawPfStrsWithPos
      .map {
        case Positioned(pos, rawPfStr) =>
          val (pfStr, pfVerOpt) = split(rawPfStr)
          Platform.parse(Platform.normalize(pfStr))
            .toRight(new MalformedPlatformError(pfStr))
            .flatMap {
              case Platform.JVM =>
                pfVerOpt match {
                  case None =>
                    val options = BuildOptions(
                      scalaOptions = ScalaOptions(
                        platform = Some(scala.build.Positioned(pos, Platform.JVM))
                      )
                    )
                    Right(options)
                  case Some(_) =>
                    Left(new UnexpectedJvmPlatformVersionError)
                }
              case Platform.JS =>
                val options = BuildOptions(
                  scalaOptions = ScalaOptions(
                    platform = Some(Positioned(pos, Platform.JS))
                  ),
                  scalaJsOptions = ScalaJsOptions(
                    version = pfVerOpt
                  )
                )
                Right(options)
              case Platform.Native =>
                val options = BuildOptions(
                  scalaOptions = ScalaOptions(
                    platform = Some(Positioned(pos, Platform.Native))
                  ),
                  scalaNativeOptions = ScalaNativeOptions(
                    version = pfVerOpt
                  )
                )
                Right(options)
            }
      }
      .sequence
      .left.map(CompositeBuildException(_)).map {
        buildOptions =>
          val mergedBuildOption = buildOptions.foldLeft(BuildOptions())(_ orElse _)
          val platforms         = buildOptions.flatMap(_.scalaOptions.platform.toSeq).distinct
          mergedBuildOption.copy(
            scalaOptions = mergedBuildOption.scalaOptions.copy(
              extraPlatforms =
                mergedBuildOption.scalaOptions.extraPlatforms ++ platforms.tail.map(p =>
                  p.value -> Positioned(p.positions, ())
                ).toMap
            )
          )
      }

  def keys = Seq("platform", "platforms")
  def handleValues(
    scopedDirective: ScopedDirective,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] =
    checkIfValuesAreExpected(scopedDirective).flatMap { groupedvaluesContainer =>
      constructBuildOptions(groupedvaluesContainer.scopedStringValues.map(_.positioned))
    }.map(buildOptions => ProcessedDirective(Some(buildOptions), Seq.empty))

}
