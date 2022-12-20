package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.directives.*
import scala.build.errors.{BuildException, MalformedPlatformError}
import scala.build.options.{BuildRequirements, Platform}
import scala.build.{Positioned, options}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Platform")
@DirectivePrefix("target.")
@DirectiveDescription("Require a Scala platform for the current file")
@DirectiveExamples("//> using target.platform \"scala-js\"")
@DirectiveExamples("//> using target.platform \"scala-js\", \"scala-native\"")
@DirectiveExamples("//> using target.platform \"jvm\"")
@DirectiveUsage(
  "//> using target.platform _platform_",
  "`//> using target.platform `_platform_"
)
@DirectiveLevel(SpecificationLevel.RESTRICTED)
// format: off
final case class RequirePlatform(
  @DirectiveName("platform")
    platforms: List[Positioned[String]] = Nil
) extends HasBuildRequirements {
  // format: on
  def buildRequirements: Either[BuildException, BuildRequirements] = either {
    val platformSet = value {
      Platform.parseSpec(platforms.map(_.value).map(options.Platform.normalize)).toRight {
        new MalformedPlatformError(
          platforms.map(_.value).mkString(", "),
          Positioned.sequence(platforms).positions
        )
      }
    }
    BuildRequirements(
      platform = Seq(BuildRequirements.PlatformRequirement(platformSet))
    )
  }
}

object RequirePlatform {
  val handler: DirectiveHandler[RequirePlatform] = DirectiveHandler.derive
}
