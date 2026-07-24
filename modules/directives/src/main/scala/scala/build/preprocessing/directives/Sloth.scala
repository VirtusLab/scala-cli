package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, PostBuildOptions}
import scala.cli.commands.SpecificationLevel

@DirectiveExamples("//> using sloth")
@DirectiveUsage("//> using sloth", "`//> using sloth`")
@DirectiveDescription(
  "Patch Scala 3.0-3.7.x lazy val bytecode on the classpath for JDK 26+ compatibility"
)
@DirectiveLevel(SpecificationLevel.EXPERIMENTAL)
final case class Sloth(
  @DirectiveName("lazyvalgrade")
  @DirectiveName("lazyValPatching")
  sloth: Boolean = false
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] =
    Right(BuildOptions(
      notForBloopOptions = PostBuildOptions(slothOpt = Some(true))
    ))
}

object Sloth {
  val handler: DirectiveHandler[Sloth] = DirectiveHandler.derive
}
