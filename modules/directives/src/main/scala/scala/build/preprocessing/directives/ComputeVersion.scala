package scala.build.preprocessing.directives

import scala.build.EitherCps.{either, value}
import scala.build.Ops.EitherOptOps
import scala.build.Positioned
import scala.build.directives.{
  DirectiveDescription,
  DirectiveExamples,
  DirectiveGroupName,
  DirectiveLevel,
  DirectiveUsage,
  HasBuildOptions
}
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ComputeVersion as cv, SourceGeneratorOptions}
import scala.cli.commands.SpecificationLevel

@DirectiveGroupName("Compute Version")
@DirectiveExamples("//> using computeVersion git")
@DirectiveExamples("//> using computeVersion git:tag")
@DirectiveExamples("//> using computeVersion git:dynver")
@DirectiveUsage("//> using computeVersion git:tag", "`//> using computeVersion` _method_")
@DirectiveDescription("Method used to compute the version for BuildInfo")
@DirectiveLevel(SpecificationLevel.RESTRICTED)
final case class ComputeVersion(computeVersion: Option[Positioned[String]] = None)
    extends HasBuildOptions {

  def buildOptions: Either[BuildException, BuildOptions] = either {
    BuildOptions(
      sourceGeneratorOptions = SourceGeneratorOptions(
        computeVersion = value {
          computeVersion
            .map(cv.parse)
            .sequence
        }
      )
    )
  }
}

object ComputeVersion {
  val handler: DirectiveHandler[ComputeVersion] = DirectiveHandler.derive
}
