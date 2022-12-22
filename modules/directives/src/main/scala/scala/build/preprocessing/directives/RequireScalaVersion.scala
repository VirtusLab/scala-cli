package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.BuildRequirements
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Scala version")
@DirectivePrefix("target.")
@DirectiveDescription("Require a Scala version for the current file")
@DirectiveExamples("//> using target.scala \"3\"")
@DirectiveUsage(
  "//> using target.scala _version_",
  "`//> using target.scala `_version_"
)
@DirectiveLevel(SpecificationLevel.RESTRICTED)
final case class RequireScalaVersion(
  scala: Option[DirectiveValueParser.MaybeNumericalString] = None
) extends HasBuildRequirements {
  def buildRequirements: Either[BuildException, BuildRequirements] = {
    val requirements = BuildRequirements(
      scalaVersion = scala.toSeq.map { ns =>
        BuildRequirements.VersionEquals(ns.value, loose = true)
      }
    )
    Right(requirements)
  }
}

object RequireScalaVersion {
  val handler: DirectiveHandler[RequireScalaVersion] = DirectiveHandler.derive
}
