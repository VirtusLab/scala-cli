package scala.build.preprocessing.directives
import scala.build.Logger
import scala.build.Ops._
import scala.build.errors.{BuildException, CompositeBuildException, MalformedPlatformError}
import scala.build.options.{BuildRequirements, Platform}
import scala.build.preprocessing.Scoped
import scala.build.Positioned

case object RequirePlatformsDirectiveHandler
    extends BuildRequirementsHandler(AtLeastOne(DirectiveValue.String)) {
  def name        = "Platform"
  def description = "Require a Scala platform for the current file"
  def usagesCode  = Seq("<platform>", "<platform1>, <platform2>")
  override def examples = Seq(
    "//> using target.platform \"scala-js\"",
    "//> using target.platform \"scala-js\", \"scala-native\"",
    "//> using target.platform \"jvm\""
  )

  def keys: Seq[String] = Seq("target.platform")

  def process(values: ::[Positioned[String]])(using Ctx) =
    values.map { platformString =>
      Platform.parse(Platform.normalize(platformString.value)) match
        case None =>
          platformString.error(
            "Invalid platform, supported: `js`, `jvm` and `native`"
          ) // TODO reuse?
        case Some(platform) => Right(platform)
    }.sequenceToComposite.map { platforms =>
      BuildRequirements(platform = Seq(BuildRequirements.PlatformRequirement(platforms.toSet)))
    }
}
