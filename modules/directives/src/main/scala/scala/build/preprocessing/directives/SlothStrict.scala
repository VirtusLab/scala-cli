package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, PostBuildOptions}
import scala.cli.commands.SpecificationLevel

@DirectiveExamples("//> using slothStrict")
@DirectiveUsage("//> using slothStrict", "`//> using slothStrict`")
@DirectiveDescription(
  "Fail when Sloth cannot resolve class hierarchies while patching dependency jars (requires --sloth or --sloth-agent)"
)
@DirectiveLevel(SpecificationLevel.EXPERIMENTAL)
final case class SlothStrict(
  @DirectiveName("lazyvalgradeStrict")
  @DirectiveName("lazyValPatchingStrict")
  slothStrict: Boolean = false
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] =
    Right(BuildOptions(
      notForBloopOptions = PostBuildOptions(slothStrictOpt = Some(slothStrict))
    ))
}

object SlothStrict {
  val handler: DirectiveHandler[SlothStrict] = DirectiveHandler.derive
}
