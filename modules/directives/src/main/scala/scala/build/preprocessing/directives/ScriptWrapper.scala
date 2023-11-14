package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ScriptOptions}
import scala.cli.commands.SpecificationLevel

@DirectiveExamples("//> using objectWrapper")
@DirectiveExamples("//> using delayedInitWrapper")
@DirectiveUsage(
  "//> using delayedInitWrapper",
  """`//> using objectWrapper`
    |
    |`//> using delayedInitWrapper`
    |""".stripMargin
)
@DirectiveLevel(SpecificationLevel.RESTRICTED)
@DirectiveDescription("Set parameters for the code wrapper for scripts")
final case class ScriptWrapper(
  @DirectiveName("object.wrapper")
  @DirectiveName("wrapper.object")
  objectWrapper: Boolean = false,
  @DirectiveName("wrapper.delayedInit")
  delayedInitWrapper: Boolean = false
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] =
    val options = BuildOptions(scriptOptions =
      ScriptOptions(
        forceObjectWrapper = Some(objectWrapper),
        forceDelayedInitWrapper = Some(delayedInitWrapper)
      )
    )
    Right(options)
}

object ScriptWrapper {
  val handler: DirectiveHandler[ScriptWrapper] = DirectiveHandler.derive
}
