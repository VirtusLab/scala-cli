package scala.build.preprocessing.directives

import scala.build.directives._
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ScriptOptions}
import scala.cli.commands.SpecificationLevel

@DirectiveExamples("//> using objectWrapper")
@DirectiveUsage("//> using objectWrapper", "`//> using objectWrapper`")
@DirectiveDescription("Set the default code wrapper for scripts to object wrapper")
@DirectiveLevel(SpecificationLevel.EXPERIMENTAL)
final case class ObjectWrapper(
  @DirectiveName("object.wrapper")
  @DirectiveName("wrapper.object")
  objectWrapper: Boolean = false
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] =
    val options = BuildOptions(scriptOptions =
      ScriptOptions(forceObjectWrapper = Some(true))
    )
    Right(options)
}

object ObjectWrapper {
  val handler: DirectiveHandler[ObjectWrapper] = DirectiveHandler.derive
}
