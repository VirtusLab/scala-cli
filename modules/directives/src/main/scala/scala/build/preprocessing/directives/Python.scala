package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, PostBuildOptions}
import scala.build.preprocessing.ScopePath
import scala.cli.commands.SpecificationLevel

@DirectiveExamples("//> using python")
@DirectiveUsage("//> using python", "`//> using python")
@DirectiveDescription("Enable Python support")
@DirectiveLevel(SpecificationLevel.EXPERIMENTAL)
final case class Python(
  python: Boolean = false
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] = {
    val options = BuildOptions(
      notForBloopOptions = PostBuildOptions(
        python = Some(true)
      )
    )
    Right(options)
  }
}

object Python {
  val handler: DirectiveHandler[Python] = DirectiveHandler.derive
}
