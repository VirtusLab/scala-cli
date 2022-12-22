package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.Ops.*
import scala.build.directives.*
import scala.build.errors.{
  BuildException,
  CompositeBuildException,
  MalformedPlatformError,
  UnexpectedJvmPlatformVersionError
}
import scala.build.options.{
  BuildOptions,
  ConfigMonoid,
  ScalaJsOptions,
  ScalaNativeOptions,
  ScalaOptions
}
import scala.build.{Logger, Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Platform")
@DirectiveExamples("//> using platform \"scala-js\"")
@DirectiveExamples("//> using platform \"jvm\", \"scala-native\"")
@DirectiveUsage(
  "//> using platform (jvm|scala-js|scala-native)+",
  "`//> using platform `(`jvm`|`scala-js`|`scala-native`)+"
)
@DirectiveDescription("Set the default platform to Scala.js or Scala Native")
@DirectiveLevel(SpecificationLevel.SHOULD)
// format: off
final case class Platform(
  @DirectiveName("platform")
    platforms: List[Positioned[String]] = Nil
) extends HasBuildOptions {
  // format: on

  private def split(input: String): (String, Option[String]) = {
    val idx = input.indexOf(':')
    if (idx < 0) (input, None)
    else (input.take(idx), Some(input.drop(idx + 1)))
  }

  def buildOptions: Either[BuildException, BuildOptions] = either {

    val allBuildOptions = value {
      platforms
        .map { input =>
          val (pfStr, pfVerOpt) = split(input.value)
          options.Platform.parse(options.Platform.normalize(pfStr)) match {
            case None =>
              Left(new MalformedPlatformError(pfStr, positions = input.positions))
            case Some(pf) =>
              (pf, pfVerOpt) match {
                case (_, None) =>
                  Right(
                    BuildOptions(
                      scalaOptions = ScalaOptions(
                        platform = Some(input.map(_ => pf))
                      )
                    )
                  )
                case (options.Platform.JVM, Some(ver)) =>
                  Left(new UnexpectedJvmPlatformVersionError(ver, input.positions))
                case (options.Platform.JS, Some(ver)) =>
                  Right(
                    BuildOptions(
                      scalaOptions = ScalaOptions(
                        platform = Some(input.map(_ => pf))
                      ),
                      scalaJsOptions = ScalaJsOptions(
                        version = Some(ver)
                      )
                    )
                  )
                case (options.Platform.Native, Some(ver)) =>
                  Right(
                    BuildOptions(
                      scalaOptions = ScalaOptions(
                        platform = Some(input.map(_ => pf))
                      ),
                      scalaNativeOptions = ScalaNativeOptions(
                        version = Some(ver)
                      )
                    )
                  )
              }
          }
        }
        .sequence
        .left.map(CompositeBuildException(_))
    }

    allBuildOptions.headOption.fold(BuildOptions()) { buildOptions =>
      val mergedBuildOptions = allBuildOptions.foldLeft(BuildOptions())(_ orElse _)
      val mainPlatformOpt =
        mergedBuildOptions.scalaOptions.platform.map(_.value) // shouldn't be empty…
      val extraPlatforms = ConfigMonoid.sum {
        allBuildOptions
          .flatMap(_.scalaOptions.platform.toSeq)
          .filter(p => !mainPlatformOpt.contains(p.value))
          .map(p => Map(p.value -> p.map(_ => ())))
      }
      mergedBuildOptions.copy(
        scalaOptions = mergedBuildOptions.scalaOptions.copy(
          extraPlatforms = mergedBuildOptions.scalaOptions.extraPlatforms ++ extraPlatforms
        )
      )
    }
  }
}

object Platform {
  val handler: DirectiveHandler[Platform] = DirectiveHandler.derive
}
