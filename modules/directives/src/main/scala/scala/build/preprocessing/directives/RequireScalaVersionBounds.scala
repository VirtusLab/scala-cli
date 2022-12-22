package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.BuildRequirements
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Scala version bounds")
@DirectivePrefix("target.scala.")
@DirectiveDescription("Require a Scala version for the current file")
@DirectiveExamples("//> using target.scala.>= \"2.13\"")
@DirectiveExamples("//> using target.scala.< \"3.0.2\"")
@DirectiveUsage(
  "//> using target.scala.>= _version_",
  "`//> using target.scala.>= `_version_"
)
@DirectiveLevel(SpecificationLevel.RESTRICTED)
final case class RequireScalaVersionBounds(
  `==`: Option[DirectiveValueParser.MaybeNumericalString] = None,
  `>=`: Option[DirectiveValueParser.MaybeNumericalString] = None,
  `<=`: Option[DirectiveValueParser.MaybeNumericalString] = None,
  `>`: Option[DirectiveValueParser.MaybeNumericalString] = None,
  `<`: Option[DirectiveValueParser.MaybeNumericalString] = None
) extends HasBuildRequirements {
  def buildRequirements: Either[BuildException, BuildRequirements] = {
    val versionRequirements =
      `==`.toSeq.map { ns =>
        BuildRequirements.VersionEquals(ns.value, loose = true)
      } ++
        `>=`.toSeq.map { ns =>
          BuildRequirements.VersionHigherThan(ns.value, orEqual = true)
        } ++
        `<=`.toSeq.map { ns =>
          BuildRequirements.VersionLowerThan(ns.value, orEqual = true)
        } ++
        `>`.toSeq.map { ns =>
          BuildRequirements.VersionHigherThan(ns.value, orEqual = false)
        } ++
        `<`.toSeq.map { ns =>
          BuildRequirements.VersionLowerThan(ns.value, orEqual = false)
        }
    val requirements = BuildRequirements(
      scalaVersion = versionRequirements
    )
    Right(requirements)
  }
}

object RequireScalaVersionBounds {
  val handler: DirectiveHandler[RequireScalaVersionBounds] = DirectiveHandler.derive
}
