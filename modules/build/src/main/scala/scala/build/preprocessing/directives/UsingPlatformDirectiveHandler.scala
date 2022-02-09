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
import scala.build.preprocessing.ScopePath
import scala.build.{Logger, Positioned}

case object UsingPlatformDirectiveHandler extends UsingDirectiveHandler {
  def name             = "Platform"
  def description      = "Set the default platform to Scala.JS or Scala Native"
  def usage            = "//> using platform (jvm|scala-js|scala-native)+"
  override def usageMd = "`//> using platform `(`jvm`|`scala-js`|`scala-native`)+"
  override def examples = Seq(
    "//> using platform \"scala-js\"",
    "//> using platform \"jvm\", \"scala-native\""
  )

  private def split(input: String): (String, Option[String]) = {
    val idx = input.indexOf(':')
    if (idx < 0) (input, None)
    else (input.take(idx), Some(input.drop(idx + 1)))
  }

  private def handle(
    rawPfStrsWithPos: Seq[Positioned[String]]
  ): Either[BuildException, BuildOptions] = either {
    val options = value {
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
        .left.map(CompositeBuildException(_))
    }

    val merged    = options.foldLeft(BuildOptions())(_ orElse _)
    val platforms = options.flatMap(_.scalaOptions.platform.toSeq).distinct
    merged.copy(
      scalaOptions = merged.scalaOptions.copy(
        extraPlatforms = merged.scalaOptions.extraPlatforms ++ platforms.tail.map(p =>
          (p.value -> Positioned(p.positions, ()))
        ).toMap
      )
    )
  }

  def keys = Seq("platform", "platforms")
  def handleValues(
    directive: StrictDirective,
    path: Either[String, os.Path],
    cwd: ScopePath,
    logger: Logger
  ): Either[BuildException, ProcessedUsingDirective] = {
    val values = directive.values
    handle(DirectiveUtil.stringValues(values, path, cwd).map(_._1))
      .map(v => ProcessedDirective(Some(v), Seq.empty))
  }
}
