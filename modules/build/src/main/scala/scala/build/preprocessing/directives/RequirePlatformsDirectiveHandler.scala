package scala.build.preprocessing.directives

import scala.build.errors.BuildException
import scala.build.options.{BuildRequirements, Platform}

case object RequirePlatformsDirectiveHandler extends RequireDirectiveHandler {
  def name             = "Platform"
  def description      = "Require a Scala platform for the current file"
  def usage            = "require _platform_"
  override def usageMd = "`require `_platform_"
  override def examples = Seq(
    "require scala-js",
    "require scala-js scala-native",
    "require jvm"
  )

  def handle(directive: Directive): Option[Either[BuildException, BuildRequirements]] =
    Platform.parseSpec(directive.values.map(Platform.normalize)) match {
      case Some(platforms) =>
        val reqs = BuildRequirements(
          platform = Seq(BuildRequirements.PlatformRequirement(platforms))
        )
        Some(Right(reqs))
      case None =>
        None
    }
}
