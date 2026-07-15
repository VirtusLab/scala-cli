package scala.build.preprocessing.directives

import scala.build.directives.*
import scala.build.errors.BuildException
import scala.build.options.{BuildOptions, ScriptOptions}
import scala.cli.commands.SpecificationLevel

@DirectiveExamples("//> using dollarWrapper")
@DirectiveUsage("//> using dollarWrapper", "`//> using dollarWrapper`")
@DirectiveDescription(
  "Use the legacy dollar-sign ($) based script wrapper naming"
)
@DirectiveLevel(SpecificationLevel.IMPLEMENTATION)
final case class DollarWrapper(
  @DirectiveName("dollar.wrapper")
  @DirectiveName("wrapper.dollar")
  dollarWrapper: Boolean = false
) extends HasBuildOptions {
  def buildOptions: Either[BuildException, BuildOptions] =
    val options = BuildOptions(scriptOptions =
      ScriptOptions(useDollarScriptWrapper = Some(true))
    )
    Right(options)
}

object DollarWrapper {
  val handler: DirectiveHandler[DollarWrapper] = DirectiveHandler.derive
}
