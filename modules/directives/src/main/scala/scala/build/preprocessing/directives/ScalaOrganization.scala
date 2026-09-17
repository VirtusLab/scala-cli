package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ScalaOptions}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Scala organization")
@DirectiveExamples("//> using scalaOrganization ch.epfl.lara")
@DirectiveUsage(
  "//> using scalaOrganization _organization_",
  "`//> using scalaOrganization` _organization_"
)
@DirectiveDescription(
  "Set the organization the Scala toolchain artifacts are fetched from (org.scala-lang by default)"
)
@DirectiveLevel(SpecificationLevel.RESTRICTED)
final case class ScalaOrganization(
  scalaOrganization: Option[String] = None
) extends HasBuildOptions:
  def buildOptions: Either[BuildException, BuildOptions] =
    Right(BuildOptions(scalaOptions = ScalaOptions(scalaOrganization = scalaOrganization)))

object ScalaOrganization:
  val handler: DirectiveHandler[ScalaOrganization] = DirectiveHandler.derive
