package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.BuildOptions
import scala.cli.commands.SpecificationLevel

@DirectiveExamples("//> using generator")
@DirectiveUsage("//> using generator", "`//> using generator`")
@DirectiveDescription("Generate code using Source Generator")
@DirectiveLevel(SpecificationLevel.EXPERIMENTAL)
final case class SourceGenerator (
    placeHolderGenerator: Boolean = false
) extends HasBuildOptions {
    def buildOptions: Either[BuildException, BuildOptions] = {
        val buildOpt = BuildOptions(
            generateSource = Some(placeHolderGenerator)
        )
        Right(buildOpt)
    }
}


object SourceGenerator {
  val handler: DirectiveHandler[SourceGenerator] = DirectiveHandler.derive
}
