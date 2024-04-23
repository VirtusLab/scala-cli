package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.internal.Constants
import scala.build.options.{BuildOptions, PostBuildOptions, ScalaNativeOptions}
import scala.build.preprocessing.ScopePath
import scala.cli.commands.SpecificationLevel

@DirectiveExamples("//> using python")
@DirectiveUsage("//> using python", "`//> using python`")
@DirectiveDescription("Enable Python support")
@DirectiveLevel(SpecificationLevel.EXPERIMENTAL)
final case class Python(
  python: Boolean = false
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] = {
    val options = BuildOptions(
      notForBloopOptions = PostBuildOptions(
        python = Some(true)
      ),
      scalaNativeOptions = ScalaNativeOptions(
        maxDefaultNativeVersions =
          List(Constants.scalaPyMaxScalaNative -> Python.maxScalaNativeWarningMsg)
      )
    )
    Right(options)
  }
}

object Python {
  val handler: DirectiveHandler[Python] = DirectiveHandler.derive
  val maxScalaNativeWarningMsg =
    s"ScalaPy does not support Scala Native ${Constants.scalaNativeVersion}, ${Constants.scalaPyMaxScalaNative} should be used instead."
}
