package scala.build.preprocessing.directives

import scala.build.options.{BuildRequirements, Platform}

case object RequirePlatformsDirectiveHandler extends RequireDirectiveHandler {
  def handle(directive: Directive): Option[Either[String, BuildRequirements]] =
    Platform.parseSpec(directive.values.map(Platform.normalize)) match {
      case Some(platforms) =>
        val reqs = BuildRequirements(
          platform = Some(BuildRequirements.PlatformRequirement(platforms))
        )
        Some(Right(reqs))
      case None =>
        None
    }
}
