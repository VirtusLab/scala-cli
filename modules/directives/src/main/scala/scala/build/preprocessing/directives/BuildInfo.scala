package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, SourceGeneratorOptions}
import scala.cli.commands.SpecificationLevel

@DirectiveExamples("//> using buildInfo")
@DirectiveUsage("//> using buildInfo", "`//> using buildInfo`")
@DirectiveDescription("Generate BuildInfo for project")
@DirectiveLevel(SpecificationLevel.RESTRICTED)
final case class BuildInfo(
  buildInfo: Boolean = false
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] =
    val options = BuildOptions(sourceGeneratorOptions =
      SourceGeneratorOptions(useBuildInfo = Some(buildInfo))
    )
    Right(options)
}

object BuildInfo {
  val handler: DirectiveHandler[BuildInfo] = DirectiveHandler.derive
}
