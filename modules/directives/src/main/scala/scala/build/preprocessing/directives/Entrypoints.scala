package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, PostBuildOptions}
import scala.cli.commands.SpecificationLevel

@DirectiveExamples("//> using entrypoints")
@DirectiveUsage("//> using entrypoints", "`//> using entrypoints`")
@DirectiveDescription("Enable entrypoint annotation support")
@DirectiveLevel(SpecificationLevel.EXPERIMENTAL)
final case class Entrypoints(
  entrypoints: Boolean = false
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] = {
    val options = BuildOptions(
      notForBloopOptions = PostBuildOptions(
        entrypoints = Some(true)
      )
    )
    Right(options)
  }
}

object Entrypoints {
  val handler: DirectiveHandler[Entrypoints] = DirectiveHandler.derive
}
